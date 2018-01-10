# Why [CryptoMilli](https://www.cryptomilli.com) - rant

As writing code is fun, I didn't write this down until the site went live: [https://www.cryptomilli.com](https://www.cryptomilli.com)

So here we go. There are a bunch of services (websites) / apps where you can buy and hold your coins with sexy dashboards.
But this imho defeats the purpose of bothering with cryptocurrencies. You should not hold all your coins and rely on a single point of failure. Ideally you should have multiple storages hardware, cold, hot (website, apps,...), where if you lost the funds in one medium the damage is not material.

So the heck with CryptoMilli:

1. public addresses are saved on your browser's local-storage. (you can go back to the site and they will still be there under normal browsing)
2. No ads, no tracking of any kind.
3. No backend: compiled version of this on a S3 bucket.
4. Open source, here you are.
5. Cool visuals, will be sexier promised :)

# Limitations

* Not sexy yet. Needs serious UI work. Will work on it, when I pick up the project again.

> Dev Status [on hold]

Please don't type your address character by character (I cringe at the thought). Paste then enter.

> I started the project with a lighthead but with the effort it took, I might be suffering of sunk cost fallacy. I still got love for it though :)

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build

To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```

> min compilation is set to :simple. (:advanced breaks rid3)
