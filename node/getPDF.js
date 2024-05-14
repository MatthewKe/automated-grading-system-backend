const puppeteer = require('puppeteer');
const PDFDocument = require('pdfkit');
const {createWriteStream} = require("fs");

const url = "http://localhost:5173/#/"
const produceUrl = 'http://localhost:5173/#/produce?project_id='
let projectId = process.argv[3]
let jwtToken = process.argv[2]

const sizes = {
    A3: {width: 420, height: 297},
    A4: {width: 297, height: 210}
}

async function getPDF(url, projectId, jwtToken) {
    const browser = await puppeteer.launch({headless: true});
    const page = await browser.newPage();
    await page.goto(url, {waitUntil: 'networkidle0'});
    await page.evaluate((token) => {
        localStorage.setItem('jwt', token);
    }, jwtToken)

    await page.goto(produceUrl + projectId, {waitUntil: 'networkidle0'});

    await page.setViewport({width: 1920, height: 1080, deviceScaleFactor: 5});  // deviceScaleFactor 提高像素密度
    await page.waitForSelector('.sheet', {visible: true});
    const elements = await page.$$('.sheet');
    await new Promise(r => setTimeout(r, 1000))
    const bufferArr = []
    for (const element of elements) {
        let buffer = await element.screenshot(); // 对元素进行截图
        bufferArr.push(buffer)
    }
    const doc = new PDFDocument({
        size: [sizes.A3.width * 2.83465, sizes.A3.height * 2.83465],
        margin: 0 // 设置边距为 0，以便图像可以填满整个页面
    });

    // 创建文件写入流
    doc.pipe(process.stdout);

    // 将图像添加到页面，调整尺寸以填满整个页面
    doc.image(bufferArr[0], 0, 0, {
        width: doc.page.width,  // 设置图像宽度为页面宽度
        height: doc.page.height // 设置图像高度为页面高度
    });

    if (bufferArr.length > 1) {
        for (let i = 1; i < bufferArr.length; i++) {
            doc.addPage({
                size: [sizes.A3.width * 2.83465, sizes.A3.height * 2.83465],
                margin: 0
            });

            // 在新的一页添加第二张图像
            doc.image(bufferArr[i], 0, 0, {
                width: doc.page.width,  // 设置图像宽度为页面宽度
                height: doc.page.height // 设置图像高度为页面高度
            });
        }
    }

    // 完成文档创建
    doc.end();

    await browser.close();
}

getPDF(url, projectId, jwtToken)

