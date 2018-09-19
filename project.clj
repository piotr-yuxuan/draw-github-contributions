(defproject draw-github-contributions "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "GNU GPL, version 3, 29 June 2007"
            :url "https://www.gnu.org/licenses/gpl-3.0.txt"
            :addendum "GPL_ADDITION.txt"}
  :dependencies [[org.clojure/clojure "1.10.0-alpha8"]
                 [org.clojure/test.check "0.10.0-alpha3"]]
  :main draw-github-contributions.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
