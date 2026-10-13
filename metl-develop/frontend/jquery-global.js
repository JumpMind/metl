// Vite 7 bundles jQuery as a CommonJS module, which skips setting the
// window globals that the diagram scripts use
import jQuery from './jquery-3.7.1.min.js';
window.jQuery = window.$ = jQuery;
