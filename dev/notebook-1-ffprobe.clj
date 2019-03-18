(ns notebook-1-ffprobe)

(require '[clojure.pprint :as pprint])
(require '[clojure.tools.namespace.repl :as tnr])

(require '[offset.etl :as etl])
(require '[offset.file :as file])

(require '[utils.json :as json])

(def files (file/mp3-file-seq "/mnt/d/Music/Collections/Performance"))
(count files)

(spit "ffprobe.json" (json/emit-str (etl/ffprobe-df files)))

; check

(count (json/parse-str (slurp "ffprobe.json")))

