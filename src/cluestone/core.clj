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

(def ktk-url "http://copper-dog.com/mtg-generator/ktk/")
(def ktk-cards-url "http://copper-dog.com/mtg-generator/ktk/cardsMain.json")

(defn get-ktk-cards!* []
  (-> (http/get ktk-cards-url)
    :body
    (json/decode true)))

(def get-ktk-cards! (memoize get-ktk-cards!*))

(defmacro comp->
  [& fns]
  `(comp ~@(reverse fns)))

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

(defn card->img
  [{:keys [src]}]
  [:span.card [:img {:width "222" :height "319" :src src}]])

(defn cards->page
  [cards]
  (html5 {}
    [:body (map card->img cards)]))

(defn rares-first
  "Compare cards by rarity such that rares end up first."
  [{a :rarity} {b :rarity}]
  (-> (and a b (> (rarity->index a) (rarity->index b)))
    boolean))

(comment
  (require '[cluestone.core :as mtg])

  (def ktk (mtg/ktk-cards!))

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
    (spit (str (gensym "./ktk-sealed-") ".html"))))
