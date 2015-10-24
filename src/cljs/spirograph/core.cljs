(ns spirograph.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [quil.core :as q :include-macros true]
              [quil.middleware :as m])
    (:import goog.History))

;;--------------------------
;; Messing with quil

;;;; Constants
(def width 500)
(def height 500)
(def frames-per-second 30)
(def rotations-per-second 0.25)

;;;; Drawing logic
(defrecord Point [x y])
(defrecord Gear [radius-ratio offset-ratio angle])

(defn get-pos [gear]
  (let [center-x (/ width 2)
        center-y (/ height 2)
        R (/ width 2)
        r (* R (:radius-ratio gear))
        s (* r (:offset-ratio gear))
        th (:angle gear)
        ph (-> R (-) (/ r) (* th))]
    (Point.
     (-> R (- r) (* (q/cos th)) (+ (-> r (- s) (* (q/cos ph)))) (+ center-x))
     (-> R (- r) (* (q/sin th)) (+ (-> r (- s) (* (q/sin ph)))) (+ center-y)))))
     
(defn update-angle [angle]
  (+ angle (/ (* 2 Math/PI rotations-per-second) frames-per-second)))

(defn last-angle [angle]
  (- angle (/ (* 2 Math/PI rotations-per-second) frames-per-second)))
  
(defn get-period [gear]
  ;; Clojurescript does not have denominator :(
  ;(* 2 Math/PI (denominator (:radius-ratio gear))))
  (* 44 Math/PI))

(defn draw-gear [gear]
  (let [old-pos (get-pos (update-in gear [:angle] last-angle))
        new-pos (get-pos gear)]
    (q/line (:x old-pos) (:y old-pos) (:x new-pos) (:y new-pos))))

;;;; Quil drawing stuff
(defn setup[]
  (q/frame-rate frames-per-second)
  (q/background 255)
  ;; Returns initial state.
  {:angle 0
   :gear (Gear. (/ 4 7) (/ 1 3) 0)})

(defn update-state [state]
  (.log js/console (pr-str state))
  (update-in state [:gear :angle] update-angle state))

(defn draw [state]
  (draw-gear (:gear state)))

(q/defsketch hello
  :host "canvas"
  :size [width height]
  :setup setup
  :update update-state
  :draw draw
  :middleware [m/fun-mode])

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to spirograph"]
   [:div [:canvas {:id "canvas"}]]
   [:div [:a {:href "#/about"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About spirograph"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
