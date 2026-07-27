// Wrapper to load tfjs-node with fallback to WASM backend
const path = require('path');
const fs = require('fs');

// First try to load the original tfjs-node
try {
  module.exports = require('@tensorflow/tfjs-node');
  console.warn('[tfjs-node-wrapper] ✅ Loaded native tfjs-node');
} catch (nativeError) {
  console.warn('[tfjs-node-wrapper] ⚠️  Native binding failed, falling back to WASM:', nativeError.message.split('\n')[0]);
  
  // Fallback: Use WASM backend
  const tf = require('@tensorflow/tfjs');
  
  // Load WASM backend
  try {
    require('@tensorflow/tfjs-backend-wasm');
    console.warn('[tfjs-node-wrapper] ✅ Loaded WASM backend');
  } catch (wasmError) {
    console.warn('[tfjs-node-wrapper] ⚠️  WASM backend not available:', wasmError.message);
  }
  
  // Set WASM as backend if available
  try {
    tf.setBackend('wasm');
    console.warn('[tfjs-node-wrapper] ✅ Set WASM as active backend');
  } catch (backendError) {
    console.warn('[tfjs-node-wrapper] Using default TensorFlow.js backend');
  }
  
  // Export tf module with browser APIs (which should work with WASM or CPU fallback)
  module.exports = tf;
}
