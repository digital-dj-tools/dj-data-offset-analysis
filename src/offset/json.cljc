(ns offset.json
  #?(:clj
     (:require [clojure.data.json :as j]
               [clojure.string :as str])
     :cljs
     (:require [clojure.walk :as walk])))

(defn json->edn
  ([json]
   (json->edn json {:keywordize? true}))
  ([json {:keys [keywordize? default] :or {keywordize? true default {}}}]
   #?(:clj
      (if-not (str/blank? json)
        (j/read-str json :key-fn (if keywordize? keyword identity))
        default)
      :cljs
      (if-not (str/blank? json)
        (let [clj (js->clj (js/JSON.parse json))]
          (if keywordize?
            (walk/keywordize-keys clj)
            clj))
        default))))