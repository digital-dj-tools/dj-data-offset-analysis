(ns offset.edn
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

; https://github.com/clojure-cookbook/clojure-cookbook/blob/master/04_local-io/4-14_read-write-clojure-data-structures.asciidoc

(defn write-seq
  [file seq]
  (with-open [writer (io/writer file)]
    (binding [*out* writer]
      (pprint/pprint seq))))

(defn read-seq
  [file]
  (with-open [reader (java.io.PushbackReader. (io/reader file))]
    (binding [*read-eval* false]
      (read reader))))

; TODO read-seq-lazy