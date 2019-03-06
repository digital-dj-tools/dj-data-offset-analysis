(ns offset.etl
  (:require
   [cemerick.url :refer [url-decode]]
   [clojure.core.async :as async]
   [converter.map :as cm]
   [converter.rekordbox.core :as cr]
   [converter.spec :as cs]
   [converter.traktor.core :as ct]
   [converter.universal.core :as cu]
   [converter.universal.tempo :as cut]
   [huri.core :as h] ; TODO replace with clojure core?
   [kixi.stats.core :as ks]
   [offset.exec :as exec]
   [offset.json :as json]
   [offset.core :as o]
   [offset.url :as url]
   [redux.core :as r]))

(defn rename-col
  [old new df]
  (as-> df $
    (h/derive-cols {new [identity old]} $)
    (h/select-cols (remove #(= old %) (h/cols $)) $)))

(defn remove-cols
  [cols df]
  (h/select-cols (remove #(contains? (set cols) %) (h/cols df)) df))

(defn df->tuples
  [df]
  (let [header (mapv name ((comp keys first) df))]
    (->> df
         (map #(vec (vals %)))
         (cons header))))

; TODO why does huri not allow optional/sparse cols?
(defn add-col-empty-tempos-if-missing
  [collection]
  (map #(if (::cu/tempos %) % (assoc % ::cu/tempos [])) collection))

; TODO rename location-str to location-decoded (it's url decoded)
(defn add-col-location-str
  [collection]
  (h/derive-cols {:location-str [url-decode ::cu/location]} collection))

(defn add-col-first-tempo
  [collection]
  ; TODO first tempo in tempos might not be the earliest in time?
  (h/derive-cols {:first-tempo [first ::cu/tempos]} collection))

(defn nml->collection-df
  [nml]
  (as-> nml $
    (cs/decode! (ct/nml-spec) $ cs/string-transformer)
    (cs/decode! ct/library-spec $ cs/xml-transformer)
    (::cu/collection $)
    (add-col-empty-tempos-if-missing $)
    ; TODO why does huri not allow optional/sparse cols?
    (remove-cols [::cu/artist ::cu/album ::cu/bpm ::cu/total-time ::cu/markers] $)
    (add-col-location-str $)
    (add-col-first-tempo $)
    (remove-cols [::cu/tempos] $)))

(defn dj-playlists->collection-df
  [dj-playlists]
  (as-> dj-playlists $
    (cs/decode! (cr/dj-playlists-spec) $ cs/string-transformer)
    (cs/decode! cr/library-spec $ cs/xml-transformer)
    (::cu/collection $)
    (add-col-empty-tempos-if-missing $)
    (add-col-location-str $)
    (add-col-first-tempo $)
    (remove-cols [::cu/tempos] $)))

(defn collection-joined-df
  [collection-1 collection-2]
  (h/join :inner-join
          :location-str
          (rename-col :first-tempo :first-tempo-1 collection-1)
          (rename-col :first-tempo :first-tempo-2 collection-2)))

(defn add-col-offset
  [collection-joined]
  (h/derive-cols {:offset [#(o/offset %1 %2) :first-tempo-1 :first-tempo-2]} collection-joined))

(defn remove-rows-without-offset
  [collection-joined]
  (h/where {:offset [#(not (nil? %))]} collection-joined))

(defn ffprobe
  [file]
  (async/go
    (let [result (async/<! (exec/exec "ffprobe" "-print_format" "json" "-show_streams" file))]
      {:file file
       :ffprobe {:out (json/json->edn (apply str (:out result)))
                 :exit (:exit result)}})))

(defn chan->seq
  [chan]
  (lazy-seq
   (let [val (async/<!! chan)]
     (if (nil? val) nil (cons val (chan->seq chan))))))

(defn ffprobe-df
  [files]
  (->> files
       (map ffprobe)
       ; https://gist.github.com/msgodf/9365059
       ; TODO does merge make any values put on input channels 
       ; available to take *immediately* on the merged channel?
       async/merge
       chan->seq))

(defn encoder-df
  [ffprobe wsl?]
  (->> ffprobe
       ; TODO how to specify nested col directly with huri?
       (h/where {:ffprobe [#(= 0 (:exit %))]})
       (h/derive-cols {:location-str [#(url-decode (url/path->url % wsl?)) :file]
                       :encoder [#(-> % :out :streams first :tags :encoder) :ffprobe]})
       (remove-cols [:file :ffprobe])))

(defn offset-encoder-df
  [collection-joined encoder]
  (->> collection-joined
       add-col-offset
    ; no first tempo (bpm, inizio) on either joined track, 
    ; i.e. not analyzed, means no offset..
    ; so if a lot of tracks aren't analyzed, this will remove a lot of rows
       remove-rows-without-offset
       (h/join :inner-join :location-str encoder)
       (h/select-cols [:first-tempo-1 :first-tempo-2 :offset :encoder])))

(defn add-prefix
  [prefix k]
  (->> k
      name
      (str prefix)
      keyword))

(defn offset-encoder-df->flat
  [offset-encoder]
  (map
   #(merge {}
           (cm/transform-keys (select-keys (-> % :first-tempo-1) [::cut/inizio ::cut/bpm])
                              (partial add-prefix "first-tempo-1-"))
           (cm/transform-keys (select-keys (-> % :first-tempo-2) [::cut/inizio ::cut/bpm])
                              (partial add-prefix "first-tempo-2-"))
           (:offset %)
           (select-keys % [:encoder]))
   offset-encoder))

(defn offset-encoder-summary-df
  [offset-encoder]
  (h/rollup-fuse :encoder
                 {:rollup [#(transduce identity (r/fuse {:count ks/count :offset-summary ks/summary}) (map :offset %)) 
                           :offset]}
                 offset-encoder))

(defn offset-encoder-summary-df->flat
  [offset-encoder-summary]
  (map
   #(merge {}
           (select-keys % [:encoder])
           (select-keys (:rollup %) [:count])
           (-> % :rollup :offset-summary)
           )
   offset-encoder-summary))

