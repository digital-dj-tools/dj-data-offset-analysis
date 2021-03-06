(ns notebook-2-offset-encoder)

(require '[clojure.pprint :as pprint])
(require '[clojure.tools.namespace.repl :as tnr])
(require '[clojure.data.csv :as csv])
(require '[clojure.data.xml :as xml])
(require '[clojure.java.io :as io])

(require '[huri.core :as h])

(require '[kixi.stats.core :as ks])

(require '[offset.etl :as etl])
(require '[offset.edn :as edn])

(require '[utils.json :as json])

(def ffprobe (json/parse-str (slurp "ffprobe.json")))
(count ffprobe)

(def encoder (etl/encoder-df ffprobe true))
(count encoder)

(def traktor-nml (xml/parse-str
                  (slurp "collection.nml")
                  :skip-whitespace true))

(def traktor-collection (etl/nml->collection-df traktor-nml))
(count traktor-collection)

; exported from rekordbox, not from converter
(def rekordbox-dj-playlists (xml/parse-str
                             (slurp "rekordbox.xml")
                             :skip-whitespace true))

(def rekordbox-collection (etl/dj-playlists->collection-df rekordbox-dj-playlists))
(count rekordbox-collection)

(def collection-joined (etl/collection-joined-df traktor-collection rekordbox-collection))
(count collection-joined)

(def offset-encoder (etl/offset-encoder-df
                     collection-joined
                     encoder))
(count offset-encoder)

(with-open [writer (io/writer "offset-encoder-flat.csv")]
  (->> offset-encoder
       etl/offset-encoder-df->flat
       etl/df->tuples
       (csv/write-csv writer)))

(def offset-encoder-summary (etl/offset-encoder-summary-df offset-encoder))

(with-open [writer (io/writer "offset-encoder-summary-flat.csv")]
  (->> offset-encoder-summary
       etl/offset-encoder-summary-df->flat
       etl/df->tuples
       (csv/write-csv writer)))