(defproject cluestone "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "https://github.com/aperiodic/cluestone"
  :license {:name "GNU General Public License, v2"
            :url "https://gnu.org/licenses/old-licenses/gpl-2.0.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cheshire "5.3.1"]
                 [clj-http "1.0.0"]
                 [enlive "1.1.5"]
                 [hiccup "1.0.5"]]
  :plugins  [[lein-ring "0.8.11"]]
  :ring {:handler cluestone.core/random-ktk-sealed-pool-handler})
