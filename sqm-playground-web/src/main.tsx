import React from "react";
import ReactDOM from "react-dom/client";
import "./monaco";
import App from "./App";
import "./styles/app.css";

/**
 * Boots the playground frontend.
 */
ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
