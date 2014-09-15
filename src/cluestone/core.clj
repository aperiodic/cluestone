(ns cluestone.core
  (:require [clojure.pprint :refer [pprint]]
            [clojure.set :refer [rename-keys]]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [html5]]))

(def rarity-str->rarity-kw
  {"c" :common
   "u" :uncommon
   "r" :rare
   "m" :mythic})

(def rarity->index
  {:common 0, :uncommon 1, :rare 2, :mythic 3})

(def rarities [:common :uncommon :rare :mythic])

(defn keywordize-rarities
  [cards]
  (map #(update-in % [:rarity] rarity-str->rarity-kw) cards))

(defn rares-first
  "Compare cards by rarity such that rares end up first."
  [{a :rarity} {b :rarity}]
  (-> (and a b (> (rarity->index a) (rarity->index b)))
    boolean))

(defn select-rarity
  [rarity cards]
  (filter #(= (:rarity %) rarity) cards))

(defn stable-rare-first-sort
  [cards]
  (->> (for [rarity (reverse rarities)]
         (select-rarity rarity cards))
    (apply concat)))

(defn pull-by-card-pred
  [pred cards]
  (->> cards
    ((juxt (partial filter pred) (partial remove pred)))
    (apply concat)))

(defn pull-by-name-pred
  [pred cards]
  (pull-by-card-pred #(pred (:name %)) cards))

(defn pull-cards
  [names cards]
  (let [name-set (set names)
        card-pred #(contains? name-set (:name %))]
    (pull-by-card-pred card-pred cards)))

(def ktk-url "http://copper-dog.com/mtg-generator/ktk/")
(def ktk-cards-url "http://copper-dog.com/mtg-generator/ktk/cardsMain.json")

(defn get-ktk-cards!* []
  (-> (http/get ktk-cards-url)
    :body
    (json/decode true)))

(def get-ktk-cards! (memoize get-ktk-cards!*))

(defn ktk-cards!
  []
  (->> (get-ktk-cards!)
    keywordize-rarities
    (map #(rename-keys % {:title :name, :colour :color}))))

(defn make-pack
  [set-cards]
  (let [cards-by-rarity (group-by :rarity set-cards)
        {commons :common, uncommons :uncommon, rares :rare, mythics :mythic} cards-by-rarity
        mythic? (< (rand) (/ 1 8))
        the-rare (if mythic? (rand-nth mythics) (rand-nth rares))]
    (concat [the-rare]
            (take 3 (shuffle uncommons))
            (take 10 (shuffle commons)))))

(defn random-sealed-pool
  [set-cards]
  (->> (take 6 (repeatedly #(make-pack set-cards)))
    (apply concat)))

(defn card->img
  [{:keys [src]}]
  [:span.card [:img {:width "222" :height "319" :src src}]])

(defn cards->page
  [cards]
  (html5 {}
    [:body (map card->img cards)]))

(defn random-ktk-sealed-pool-handler
  [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (cards->page (->> (random-sealed-pool (ktk-cards!))
                        (sort-by :name)
                        (pull-by-name-pred #(re-find #"Banner$" %))
                        (pull-cards ["Bloodstained Mire"
                                     "Flooded Strand"
                                     "Polluted Delta"
                                     "Windswept Heath"
                                     "Sandsteppe Citadel"
                                     "Mystic Monastery"
                                     "Opulent Palace"
                                     "Nomad Outpost"
                                     "Frontier Bivouac"
                                     "Tranquil Cove"
                                     "Dismal Backwater"
                                     "Bloodfell Caves"
                                     "Rugged Highlands"
                                     "Blossoming Sands"
                                     "Scoured Barrens"
                                     "Swiftwater Cliffs"
                                     "Jungle Hollow"
                                     "Wind-Scarred Crag"
                                     "Thornwood Falls"
                                     ])
                        stable-rare-first-sort))})

(comment
  (require '[cluestone.core :as mtg])

  (def ktk (mtg/ktk-cards!))

  (->> (random-sealed-pool ktk)
    (sort-by :name)
    stable-rare-first-sort)

  (into {} (for [[rarity cards] (group-by :rarity ktk)]
             [rarity (count cards)]))

  ;; crack-a-pack!
  (->> (mtg/make-pack ktk)
    (map :name))

  ;; lookup a card by name
  (-> (group-by :name ktk)
    (get "Wooded Foothills"))

  ;; write a random sealed pool to disk as a webpage
  (->> (take 6 (repeatedly #(mtg/make-pack ktk)))
    (apply concat)
    (sort mtg/rares-first)
    mtg/cards->page
    (spit (str (gensym "./ktk-sealed-") ".html")))

  ;; ktk sealed pool sorting
  (->> (random-sealed-pool ktk)
    (sort-by :name)
    (pull-by-name-pred #(re-find #"Banner$" %))
    )
  )
