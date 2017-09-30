# nuenen

This was my 2017 summer internship project. I wrote the back-end/front-end in clojure/clojurescript. It has been modified to not require a database, but one could be easily hooked up. All of the data is generated dummy data. The idea behind this site was to make an interal facing tool that hosts our analytics team's reports, displays them, generates insights from the data, and allows you to filter and download the data.

## Please Note

Because some information has been changed to 1) Protect confidential information, 2) Keep the company this project was built for anonymous, and 3) Make this as easy to run as possible (for example, there is no database) there are a few issues. Two known issues are you can't change the participant ID and you can't download the data. Please contact me for more information on this project.


## Prerequisites

You will need [Leiningen][1] 2.0 or above installed. You will also need sassc. The easiest way to install it is "brew install sassc".

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein figwheel
    lein run

Connect to localhost:3000. The email and password I have manually included are:
    nap2152@columbia.edu
    supersecret

## License

Copyright © 2017 FIXME
