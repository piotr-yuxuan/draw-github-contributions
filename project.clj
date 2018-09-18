(defproject draw-github-contributions "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "GNU GPL, version 3, 29 June 2007"
            :url "https://www.gnu.org/licenses/gpl-3.0.txt"
            :addendum "Copyright (C) 2018 胡雨軒 Петр\n\nThis program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.\n\nThis program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.\n\nYou should have received a copy of the GNU General Public License along with this program. If not, see http://www.gnu.org/licenses/.\n\nAdditional permission under GNU GPL version 3 section 7\n\nIf you modify this Program, or any covered work, by linking or combining it with clojure (or a modified version of that library), containing parts covered by the terms of EPL, the licensors of this Program grant you additional permission to convey the resulting work. {Corresponding Source for a non-source form of such a combination shall include the source code for the parts of clojure used as well as that of the covered work.}"}
  :dependencies [[org.clojure/clojure "1.10.0-alpha4"]
                 [net.mikera/imagez "0.12.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [clj-jgit "0.8.10"]]
  :main ^:skip-aot draw-github-contributions.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
