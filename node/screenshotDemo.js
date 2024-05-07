const puppeteer = require('puppeteer');
const PDFDocument = require('pdfkit');
const {createWriteStream} = require("fs");
const {pipe} = require("pdfkit/js/pdfkit.standalone");

const url = "http://localhost:5173/#/"
const produceUrl = 'http://localhost:5173/#/produce?project_id='
const username = 'root'
const password = 'root'
let projectId = 1352

const sizes = {
    A3: {width: 420, height: 297},
    A4: {width: 297, height: 210}
}

async function loginAndGetThumbnail(url, projectId, username, password) {
    const browser = await puppeteer.launch({headless: false});
    const page = await browser.newPage();
    page.on('console', message => console.log(message.text()))
    page.on('requestfailed', request => {
        console.log(`Failed to load ${request.url()}: ${request.failure().errorText}`);
    });

    await page.goto(url);
    await page.click('a[href*="login"]');

    // 输入用户名和密码
    await page.type('#username', username);
    await page.type('#password', password);

    // 点击登录按钮
    await page.click('#login-button');

    await page.waitForNavigation();

    await page.goto(produceUrl + projectId, {waitUntil: 'networkidle0'});
    await page.setViewport({width: 1920, height: 1080, deviceScaleFactor: 10});  // deviceScaleFactor 提高像素密度

    const elements = await page.$$('.sheet');

    console.log(elements)
    const bufferArr = []
    for (const element of elements) {
        let buffer = await element.screenshot(); // 对元素进行截图
        bufferArr.push(buffer)
    }

    const doc = new PDFDocument({
        size: [420 * 2.83465, 297 * 2.83465], // A3 size in points, width then height (landscape)
        margin: 0 // 设置边距为 0，以便图像可以填满整个页面
    });


    // 创建文件写入流
    const output = createWriteStream('output.pdf');
    doc.pipe(output);

    // 将图像添加到页面，调整尺寸以填满整个页面
    doc.image(bufferArr[0], 0, 0, {
        width: doc.page.width,  // 设置图像宽度为页面宽度
        height: doc.page.height // 设置图像高度为页面高度
    });

    if (bufferArr.length > 1) {
        for (let i = 1; i < bufferArr.length; i++) {
            doc.addPage({
                size: [420 * 2.83465, 297 * 2.83465],
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

loginAndGetThumbnail(url, projectId, username, password)

