(ns offset.file
 (:require [clojure.java.io :as io]))

(defn mp3-file-seq
  [dir]
  (->> dir
       io/file
       file-seq
       (filter #(and (.isFile %) (.endsWith (.getName %) ".mp3")))
       (map #(.getPath %))))

(defn write-seq
  [file seq f]
  (with-open [writer (io/writer file)]
    (doseq [line (f seq)]
      (binding [*out* writer]
        (println line)))))