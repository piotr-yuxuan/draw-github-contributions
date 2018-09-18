(ns draw-github-contributions.core
  (:gen-class)
  (:require [mikera.image.core :as img]
            [clj-jgit.porcelain :as git]
            [clojure.spec.alpha :as spec])
  (:import (java.time LocalDate ZoneId ZonedDateTime)
           (java.util Date)
           (org.eclipse.jgit.api CommitCommand Git)
           (org.eclipse.jgit.lib PersonIdent)
           (java.awt.image BufferedImage)))

(def number-of-weeks 52)
(def number-of-weekdays 7)

(def strict-pos-int? #(and (number? %) (or (zero? %) (pos-int? %))))
(def zone-date-time? #(instance? ZonedDateTime %))

(spec/def :commits/number strict-pos-int?)
(spec/def :commits/date zone-date-time?)
(spec/def ::grid-element (spec/keys :req [:commits/number
                                          :commits/date]))
(spec/def ::no-twice-same-date (fn no-twice-same-date [grid-elements]
                                 (->> grid-elements
                                      (group-by :commits/date)
                                      vals
                                      (map count)
                                      set
                                      (= #{1}))))
(spec/def ::grid (spec/and #(= number-of-weeks (count %))
                           (spec/coll-of (spec/and (spec/coll-of ::grid-element)
                                                   ::no-twice-same-date
                                                   #(= number-of-weekdays (count %))))))
(spec/def ::colours-I-know #{-1 -16777216})
(spec/def :committer/name string?)
(spec/def :committer/email string?)
(spec/def :config/committer (spec/keys :req [:committer/name :committer/email]))
(spec/def :config/number-of-commits strict-pos-int?)
(spec/def :config/bottom-right-hand-corner-date zone-date-time?)
(spec/def :config/image-resource #(instance? BufferedImage %))
(spec/def :config/repository #(instance? Git %))
(spec/def :config/side-effects? boolean?)

(spec/def ::config (spec/keys :req [:config/number-of-commits
                                    :config/bottom-right-hand-corner-date
                                    :config/image-resource]
                              :opt [:config/committer
                                    :config/repository
                                    :config/side-effects?]))

(defn transpose [m]
  (apply mapv vector m))

(defn ->java-zoned-date-time
  (^ZonedDateTime [^Integer year ^Integer month ^Integer dayOfMonth]
   (-> (LocalDate/of year month dayOfMonth)
       (.atStartOfDay (ZoneId/of "UTC")))))

(defn ->java-date
  (^Date [^ZonedDateTime zoned-date-time]
   (-> zoned-date-time
       .toInstant
       Date/from)))

(defn generate-commits!
  [^Git repo config java-date]
  (-> repo
      ^CommitCommand (.commit)
      (.setMessage "")
      (.setAllowEmpty true) ;; explicit
      (.setCommitter (PersonIdent. (PersonIdent.
                                     ^String (-> config :config/committer :committer/name)
                                     ^String (-> config :config/committer :committer/email))
                                   ^Date java-date))
      (.call)))

(defn empty-pixel?
  "Depends on spec `::colours-I-know`."
  [pixel]
  (not= -1 pixel))

(defn get-image-pixels [image-resource]
  (->> image-resource
       img/get-pixels
       seq))

(defn assert-workable-image [image-pixels]
  (assert (and (spec/valid? (spec/coll-of ::colours-I-know)
                            image-pixels)
               #(= (count image-pixels)
                   (* number-of-weeks
                      number-of-weekdays))))
  image-pixels)

(defn fill-commits-numbers [number-of-commits image-pixels]
  (->> image-pixels
       (map (fn [pixel]
              {:commits/number (if (empty-pixel? pixel)
                                 number-of-commits
                                 0)}))))

(defn tranpose-data-representation [grid-elements]
  (->> grid-elements
       (partition number-of-weeks)
       transpose
       (mapcat identity)))

(defn fill-commits-dates [number-of-weeks number-of-weekdays reference-date grid-elements]
  (let [distance-from-reference-date #(- (* number-of-weeks
                                            number-of-weekdays)
                                         (inc %))]
    (->> grid-elements
         (map-indexed (fn [i grid-element]
                        (assoc grid-element
                          :commits/date (.minusDays reference-date (distance-from-reference-date i))))))))

(defn ->grid-elements
  [config]
  (let [{:config/keys [bottom-right-hand-corner-date image-resource number-of-commits]} config
        reference-date ^ZonedDateTime bottom-right-hand-corner-date]
    (->> (get-image-pixels image-resource)
         assert-workable-image
         (fill-commits-numbers number-of-commits)
         tranpose-data-representation
         (fill-commits-dates number-of-weeks number-of-weekdays reference-date))))

(def default-config
  {:config/committer {:committer/name "Fake Contribution"
                      :committer/email "piotr-yuxuan@users.noreply.github.com"}
   :config/number-of-commits 150
   :config/bottom-right-hand-corner-date (->java-zoned-date-time 2018 9 22)
   :config/image-resource (img/load-image-resource "contributions.png")
   :config/repository (git/load-repo "../fake-contributions")})

(defn -main
  [& {:as runtime-config}]
  (let [actual-config (merge default-config runtime-config)]
    (assert (spec/valid? ::config actual-config))
    (let [{:config/keys [side-effects? repository]} actual-config
          grid-elements (->grid-elements actual-config)]
      (assert (spec/valid? ::grid (partition number-of-weekdays grid-elements)))
      (doseq [{:commits/keys [number date]} grid-elements]
        (let [java-date (->java-date date)]
          (when side-effects?
            (doall (repeatedly number #(generate-commits! repository actual-config java-date))))))
      grid-elements)))

(comment
  ;; $ cd ../fake-contributions
  ;; $ rm -rf .git
  ;; $ git init .
  ;; $ git add .
  ;; $ git remote add origin git@github.com:piotr-yuxuan/fake-contributions.git
  (-main :config/side-effects? true)
  ;; $ git push -u origin master
  )
