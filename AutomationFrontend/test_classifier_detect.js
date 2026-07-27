const path = require('path');
const fs = require('fs');

async function testDetection() {
    console.log('🎯 Testing Visual Detection with TuEmpresa screenshots\n');

    const { createCanvas, loadImage } = require('canvas');
    const labelsDir = '/Users/jhonsebastianrianoramirez/Katalon Studio/testAndroid/Include/resources/classifier-labels';

    const testImages = [
        { file: path.join(labelsDir, 'add_to_cart_button/sample_01.png'), expectedLabel: 'add_to_cart_button' },
        { file: path.join(labelsDir, 'checkout_button/sample_01.png'), expectedLabel: 'checkout_button' },
    ];

    for (const test of testImages) {
        if (!fs.existsSync(test.file)) {
            console.log(`⚠️  File not found: ${test.file}`);
            continue;
        }

        try {
            const img = await loadImage(test.file);
            console.log(`✅ ${test.expectedLabel}: Image ${img.width}x${img.height} loaded successfully`);

            const canvas = createCanvas(img.width, img.height);
            const ctx = canvas.getContext('2d');
            ctx.drawImage(img, 0, 0);

            const buffer = canvas.toBuffer('image/png');
            console.log(`   Image buffer: ${buffer.length} bytes`);
            console.log(`   Ready for classifier.find() call during Appium session\n`);
        } catch (e) {
            console.log(`❌ ${test.expectedLabel}: ${e.message}\n`);
        }
    }

    try {
        const classifier = require('test-ai-classifier');
        console.log('✅ test-ai-classifier loaded — pipeline ready');
        console.log('   When Appium session is active, call:');
        console.log('   await classifier.find(driver, "add_to_cart_button")');
    } catch (e) {
        console.log('❌ Classifier load failed:', e.message);
    }
}

testDetection().catch(console.error);
