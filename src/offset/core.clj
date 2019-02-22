(ns offset.core
  (:require [converter.universal.tempo :as cut]))

; def bpm_period(bpm):
;     return (60.0 / bpm )

; def find_min_beat(bpm, cue):
;     period = bpm_period(bpm)

;     beats = int(cue / period)
;     ret = cue - beats * period
;     return ret

; def  find_offset(bpm1, cue1, bpm2, cue2):
;     return find_min_beat(bpm1, cue1) - find_min_beat(bpm2, cue2)

(defn period
  [bpm]
  (/ 60 bpm))

(defn beats
  [tempo-inizio period]
  (int (/ tempo-inizio period)))

(defn seconds-after-nearest-beat
  [bpm tempo-inizio]
  (let [period (period bpm)
        beats (beats tempo-inizio period)]
    (- tempo-inizio (* beats period))))

(defn offset
  [{:keys [] bpm-1 ::cut/bpm inizio-1 ::cut/inizio}
   {:keys [] bpm-2 ::cut/bpm inizio-2 ::cut/inizio}]
  (if (and bpm-1 inizio-1 bpm-2 inizio-2)
    (let [seconds-after-nearest-beat-1 (seconds-after-nearest-beat bpm-1 inizio-1)
          seconds-after-nearest-beat-2 (seconds-after-nearest-beat bpm-2 inizio-2)]
      {:seconds-after-nearest-beat-1 seconds-after-nearest-beat-1
       :seconds-after-nearest-beat-2 seconds-after-nearest-beat-2
       :offset (- seconds-after-nearest-beat-1 seconds-after-nearest-beat-2)})))