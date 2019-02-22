(ns offset.url
  (:require
   [cemerick.url :refer [url]]
   [clojure.java.io :as io]
   [clojure.string :refer [join lower-case split upper-case]]
   [converter.str :as str]))

; TODO move to converter url ns
(defn- wsl->drive
  [path wsl?]
  (if wsl?
    (clojure.string/replace path #"^/mnt/([a-z])/" #(str "/" (upper-case (% 1)) ":/"))
    path))

; TODO move to converter url ns
(defn- drive-letter
  [paths]
  (if (str/drive-letter? (first paths))
    (first paths)))

; TODO move to converter url ns
; TODO can be reused in traktor core ns
(defn- parse-path
  ([path]
   (parse-path path false))
  ([path wsl?]
   (let [paths (-> path (wsl->drive wsl?) (split #"/") rest)
         drive (drive-letter paths)]
     (cond-> {:dirs (vec (if drive (rest (drop-last paths)) (drop-last paths)))
              :file (last paths)}
       drive (assoc :drive drive)))))

(defn path->url
  [file wsl?]
  (let [path (-> file io/file io/as-url .getPath) 
        path-parsed (parse-path path wsl?)]
    (apply url (as-> ["file://localhost"] $
                   (conj $ (:drive path-parsed))
                   (reduce conj $ (:dirs path-parsed))
                   (conj $ (:file path-parsed))))))