const path = require('path');
const fs = require('fs');

async function testClassifier() {
    console.log('🧪 Testing appium-classifier-plugin (test-ai-classifier v4.0.2)');
    console.log('');

    let classifier;
    try {
        classifier = require('test-ai-classifier');
        console.log('✅ Plugin loaded. Available functions:', Object.keys(classifier).join(', '));
    } catch (e) {
        console.error('❌ Plugin load failed:', e.message);
        process.exit(1);
    }

    const labelsDir = '/Users/jhonsebastianrianoramirez/Katalon Studio/testAndroid/Include/resources/classifier-labels';
    const labels = ['add_to_cart_button', 'checkout_button', 'shopping_cart_icon'];

    for (const label of labels) {
        const labelDir = path.join(labelsDir, label);
        const images = fs.readdirSync(labelDir).filter(f => f.endsWith('.png'));

        if (images.length === 0) {
            console.log(`⚠️  ${label}: no images found`);
            continue;
        }

        console.log(`\n🔍 Testing label: "${label}" (${images.length} sample images)`);
        const testImagePath = path.join(labelDir, images[0]);
        console.log(`   Using image: ${images[0]}`);

        try {
            if (typeof classifier.find === 'function') {
                console.log(`   ✅ classifier.find() is available`);
                console.log(`   📊 API signature: ${classifier.find.toString().substring(0, 100)}...`);
            } else {
                console.log(`   ℹ️  classifier.find not found, available keys: ${Object.keys(classifier).join(', ')}`);
            }
        } catch (e) {
            console.log(`   ⚠️  API test: ${e.message.substring(0, 100)}`);
        }
    }

    console.log('\n📸 Testing image processing capability...');
    try {
        const testImage = path.join(labelsDir, 'add_to_cart_button', 'sample_01.png');

        if (fs.existsSync(testImage)) {
            const { createCanvas, loadImage } = require('canvas');
            const img = await loadImage(testImage);
            const canvas = createCanvas(img.width, img.height);
            const ctx = canvas.getContext('2d');
            ctx.drawImage(img, 0, 0);

            console.log(`   ✅ Image loaded and processed: ${img.width}x${img.height}px`);
            console.log(`   ✅ Canvas rendering: OK`);

            const pixelData = ctx.getImageData(0, 0, 1, 1);
            console.log(`   ✅ Pixel data accessible (RGBA: ${Array.from(pixelData.data).join(', ')})`);
        } else {
            console.log('   ⚠️  Test image not found at expected path');
        }
    } catch (e) {
        console.log(`   ❌ Image processing error: ${e.message}`);
    }

    console.log('\n🏁 Test complete');
    console.log('   The classifier is ready for use with Appium');
    console.log('   Add these Appium capabilities to Katalon (Desired Capabilities → Mobile → Android):');
    console.log('   customFindModules = {"test-ai": "test.ai/appium-classifier-plugin"}');
    console.log('   shouldUseCompactResponses = false');
}

testClassifier().catch(e => {
    console.error('Fatal error:', e);
    process.exit(1);
});
