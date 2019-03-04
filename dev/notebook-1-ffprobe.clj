(ns notebook-1-ffprobe)

(require '[clojure.pprint :as pprint])
(require '[clojure.tools.namespace.repl :as tnr])

(require '[offset.etl :as etl])
(require '[offset.edn :as edn])
(require '[offset.file :as file])

(def sample-files (file/mp3-file-seq "/mnt/d/Music/Collections/Performance"))
(count sample-files)

(edn/write-seq "sample-ffprobe-df.edn" (etl/ffprobe-df sample-files))

; check

(count (edn/read-seq "sample-ffprobe-df.edn"))

