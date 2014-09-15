# cluestone

If you like clojure and Magic: the Gathering, then you're in luck!

## Cut to the chase!

Start a webserver that generates Khans of Tarkir sealed pools as printable webpages by cloning this repo and then running
```clj
lein ring server
```

It should open up in your browser, but if not you can find it at `http://localhost:3000`.

## Tell me more...

I mean, if you are able to like those things then your life is pretty good already.
Anyways, *this library* is just something I threw together in half an hour in order to generate Khans of Tarkir sealed pools to print out and proxy up.

## Usage

It's just a handful of functions to get the data from [Cam Marsollier's site](http://copper-dog.com/mtg-generator/ktk/), make packs from the set description, and create a simple webpage to print out using a web browser as a poor-man's PDF.

Add `[cluestone 0.1.0]` to your project.clj, and fire up a repl.
Then, enter

```clj
(require '[cluestone.core :as mtg])
```

Pull down a seq of Khans of Tarkir's cards:

```clj
(def ktk (mtg/ktk-cards!))
```

Generate a pack from it:
```clj
(mtg/make-pack ktk)
```

Turn a pack into html:
```clj
(-> (mtg/make-pack ktk) mtg/cards->page)
```

Write a random sealed to disk as a webpage:
```clj
(->> (take 6 (repeatedly #(mtg/make-pack ktk)))
    (apply concat)
    (sort mtg/rares-first)
    mtg/cards->page
    (spit (str (gensym "./ktk-sealed-") ".html")))
```

## License

Copyright © 2014 Dan Lidral-Porter

Distributed under the Gnu Public License version 2.0.
See LICENSE file.
