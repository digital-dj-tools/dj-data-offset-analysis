(ns notebook-1-ffprobe)

(require '[clojure.pprint :as pprint])
(require '[clojure.tools.namespace.repl :as tnr])

(require '[offset.etl :as etl])
(require '[offset.edn :as edn])
(require '[offset.file :as file])

(def files (file/mp3-file-seq "/mnt/d/Music/Collections/Performance"))
(count files)

(edn/write-seq "ffprobe.edn" (etl/ffprobe-df files))

; check

(count (edn/read-seq "ffprobe.edn"))

