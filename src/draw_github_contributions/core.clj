(ns draw-github-contributions.core
  (:gen-class)
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.time LocalDate)
           (java.awt.image BufferedImage)
           (javax.imageio ImageIO)
           (java.io File)))

(def number-of-weeks 52)
(def number-of-weekdays 7)

(defn transpose [m]
  (apply mapv vector m))

(defn local-date->git-date
  "MM/DD/YYYY"
  [^LocalDate java-date]
  (str (.getYear java-date) "-"
       (let [month (str (.getMonthValue java-date))]
         (str (when (= 1 (count month)) "0")
              month)) "-"
       (let [day-of-month (str (.getDayOfMonth java-date))]
         (str (when (= 1 (count day-of-month)) "0")
              day-of-month))
       "T00:00:00+00:00"))

(defn generate-commits!
  "Quite slow. `jgit` would be way faster but it doesn't currently work on GraalVM."
  [config date]
  (let [committer (:config/committer config)
        author (str "--author='" (:committer/name committer) " <" (:committer/email committer) ">'")
        date (str "--date=" (local-date->git-date date))
        all-modified-files "--all"
        allow-empty "--allow-empty"
        allow-empty-message "--allow-empty-message"
        empty-message-flag "-m"
        empty-message ""
        no-gpg-sign "--no-gpg-sign"
        no-pre-commit-verification-hooks "--no-verify"]
    (if (:config/side-effects? config)
      (shell/with-sh-dir (:config/repository-path config)
        (shell/sh "git" "commit"
                  author date
                  allow-empty all-modified-files
                  allow-empty-message empty-message-flag empty-message
                  no-gpg-sign no-pre-commit-verification-hooks))
      ;; TODO Yes println is a side effect. To be renamed. The idea is
      ;; to generate a 'pure' script, then have it executed.
      (println "git" "commit"
               author date
               allow-empty all-modified-files
               allow-empty-message empty-message-flag empty-message
               no-gpg-sign no-pre-commit-verification-hooks))))

(defn empty-pixel?
  "Important domain function which is relied on by tests, spec and core
  logic."
  [pixel]
  (= [0 0 0] pixel))

(defn tranpose-data-representation [grid-elements]
  (->> grid-elements
       (partition number-of-weeks)
       transpose
       (mapcat identity)))

(defn fill-commits-dates [number-of-weeks number-of-weekdays config-date grid-elements]
  (let [[^int year ^int month ^int day-of-month] config-date
        reference-date (LocalDate/of year month day-of-month)
        distance-from-reference-date #(- (* number-of-weeks
                                            number-of-weekdays)
                                         (inc %))]
    (map-indexed (fn [i grid-element]
                   (->> i
                        distance-from-reference-date
                        (.minusDays reference-date)
                        (assoc grid-element
                          :commits/date)))
                 grid-elements)))

(defn fill-commits-number
  [number-of-commits pixel]
  {:commits/number (if (empty-pixel? pixel)
                     number-of-commits
                     0)})

(defn get-image-pixels [image-path]
  (let [^BufferedImage image (ImageIO/read ^File (io/as-file image-path))]
    (->> (.getDataElements (.getRaster image)
                           (int 0) ;; x
                           (int 0) ;; y
                           (int (.getWidth image))
                           (int (.getHeight image))
                           nil ;; just kidding bro, don't mess with it
                           )
         ^ints vec
         (partition 3))))

(defn ->grid-elements
  [config]
  (let [{:config/keys [bottom-right-hand-corner-date image-path number-of-commits]} config
        ;; way easier to read
        config-date bottom-right-hand-corner-date]
    (->> (get-image-pixels image-path)
         (map #(fill-commits-number number-of-commits %))
         tranpose-data-representation
         (fill-commits-dates number-of-weeks number-of-weekdays config-date))))

(def default-config
  {:config/committer {:committer/name "Fake Contribution"
                      :committer/email "piotr-yuxuan@users.noreply.github.com"}
   :config/number-of-commits 69
   :config/bottom-right-hand-corner-date [2019 9 7] ;; fuck you java.util.Date
   :config/image-path "resources/contributions.png"
   :config/repository-path "../fake-contributions"})

(defn -main
  [& {:as args}]
  (let [runtime-config (->> args
                            (map (fn [[k v]]
                                   [(clojure.edn/read-string k) (clojure.edn/read-string v)]))
                            (into {}))
        actual-config (merge default-config runtime-config)]
    (->> (->grid-elements actual-config)
         (mapcat (fn [{:commits/keys [number date]}]
                   (repeatedly number #(generate-commits! actual-config date))))
         doall)))
