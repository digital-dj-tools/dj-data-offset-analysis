(ns offset.exec
  (:require
   [clojure.core.async :as async]))

; TODO should be in it's own project as cljc, with clj/cljs equivalents of exec-chan

; https://gist.github.com/frankhenderson/d60471e64faec9e2158c

; https://stackoverflow.com/questions/45292625/how-to-perform-non-blocking-reading-stdout-from-a-subprocess-in-clojure

; clj impl - see above gist for cljs impl
(defn exec-chan
  "spawns a child process for cmd. routes stdout, stderr, and
  the exit code to a channel. returns the channel immediately."
  [cmd args]
  ; TODO use three channels for out, err and exit
  (let [c (async/chan)]
    (async/go
      (let [builder (ProcessBuilder. (into-array String (cons cmd args)))
            process (.start builder)]
        (with-open [reader (clojure.java.io/reader (.getInputStream process))
                    err-reader (clojure.java.io/reader (.getErrorStream process))]
          (loop []
            (let [line (.readLine ^java.io.BufferedReader reader)
                  err (.readLine ^java.io.BufferedReader err-reader)]
              (if (or line err)
                (do (when line (async/>! c [:out line]))
                    (when err (async/>! c [:err err]))
                    (recur))
                (do
                  (.waitFor process)
                  (async/>! c [:exit (.exitValue process)]))))))))
    c))

(defn exec
  "executes cmd with args. returns a channel immediately which
  will eventually receive a result map of 
  {:out [stdout-lines] :err [stderr-lines] :exit [exit-code]}"
  [cmd & args]
  (let [c (exec-chan cmd args)]
    ; TODO use async/reduce (or async/go-loop) instead of loop/recur?
    ; TODO return lazy seq on out and err channels
    (async/go (loop [output (async/<! c) result {}]
                (if (= :exit (first output))
                  (assoc result :exit (second output))
                  (recur (async/<! c) (update result (first output) #(conj (or % []) (second output)))))))))
