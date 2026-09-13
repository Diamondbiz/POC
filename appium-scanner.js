const axios = require('axios');

async function scanAppiumDevice() {
    const appiumUrl = 'http://127.0.0.1:4723';
    const deviceUdid = '192.168.1.165:5555';

    console.log('🔍 Starting Appium device scan...');
    console.log('📱 Device:', deviceUdid);
    console.log('🌐 Appium Server:', appiumUrl);
    console.log('');

    try {
        // Check Appium server status
        console.log('1️⃣ Checking Appium server status...');
        const statusResponse = await axios.get(`${appiumUrl}/status`);
        console.log('✅ Appium server is ready');
        console.log('   Version:', statusResponse.data.value.build.version);
        console.log('');

        // Create session - use proper W3C format
        console.log('2️⃣ Creating Appium session...');
        const sessionResponse = await axios.post(`${appiumUrl}/session`, {
            capabilities: {
                alwaysMatch: {
                    platformName: 'Android',
                    'appium:automationName': 'UiAutomator2'
                },
                firstMatch: [
                    {
                        'appium:deviceName': 'Android Device',
                        'appium:udid': deviceUdid,
                        'appium:noReset': true,
                        'appium:fullReset': false
                    }
                ]
            }
        });

        const sessionId = sessionResponse.data.value.sessionId;
        console.log('✅ Session created:', sessionId);
        console.log('');

        // Get context handles
        console.log('3️⃣ Fetching context handles...');
        const contextsResponse = await axios.get(`${appiumUrl}/session/${sessionId}/contexts`);
        const contexts = contextsResponse.data.value;
        console.log('📋 Available contexts:');
        contexts.forEach(ctx => console.log('   -', ctx));
        console.log('');

        // Find webview context
        const webviewContext = contexts.find(ctx => ctx.includes('WEBVIEW') || ctx.includes('webview'));
        if (!webviewContext) {
            console.log('⚠️  No webview context found');
            console.log('   Getting native page source instead...');
            console.log('');

            // Get native page source
            console.log('4️⃣ Getting native page source...');
            const sourceResponse = await axios.get(`${appiumUrl}/session/${sessionId}/source`);
            const pageSource = sourceResponse.data.value;
            console.log('✅ Page source retrieved');
            console.log('');
            console.log('═══════════════════════════════════════════════════');
            console.log('📄 NATIVE PAGE SOURCE');
            console.log('═══════════════════════════════════════════════════');
            console.log(pageSource);
            console.log('═══════════════════════════════════════════════════');
            console.log('');

            // Search for username/password fields in native
            console.log('5️⃣ Searching for username/password fields in native...');
            const usernamePatterns = [
                /txtUserId/i,
                /userId/i,
                /user/i,
                /username/i,
                /email/i,
                /login/i
            ];
            const passwordPatterns = [
                /password/i,
                /pass/i,
                /pwd/i,
                /txtUserCellPhone/i
            ];

            let usernameField = null;
            let passwordField = null;

            // Simple regex search in page source
            const lines = pageSource.split('\n');
            lines.forEach((line, index) => {
                if (!usernameField) {
                    for (const pattern of usernamePatterns) {
                        if (pattern.test(line)) {
                            usernameField = { line: index + 1, content: line.trim() };
                            break;
                        }
                    }
                }
                if (!passwordField) {
                    for (const pattern of passwordPatterns) {
                        if (pattern.test(line)) {
                            passwordField = { line: index + 1, content: line.trim() };
                            break;
                        }
                    }
                }
            });

            console.log('   Username field:', usernameField ? '✅ Found' : '❌ Not found');
            if (usernameField) {
                console.log('      Line:', usernameField.line);
                console.log('      Content:', usernameField.content.substring(0, 100));
            }
            console.log('   Password/Phone field:', passwordField ? '✅ Found' : '❌ Not found');
            if (passwordField) {
                console.log('      Line:', passwordField.line);
                console.log('      Content:', passwordField.content.substring(0, 100));
            }
            console.log('');
        } else {
            console.log('✅ Found webview context:', webviewContext);
            console.log('');

            // Switch to webview context
            console.log('4️⃣ Switching to webview context...');
            await axios.post(`${appiumUrl}/session/${sessionId}/context`, {
                name: webviewContext
            });
            console.log('✅ Switched to:', webviewContext);
            console.log('');

            // Get page source
            console.log('5️⃣ Getting page source from webview...');
            const sourceResponse = await axios.get(`${appiumUrl}/session/${sessionId}/source`);
            const pageSource = sourceResponse.data.value;
            console.log('✅ Page source retrieved');
            console.log('');
            console.log('═══════════════════════════════════════════════════');
            console.log('📄 PAGE SOURCE');
            console.log('═══════════════════════════════════════════════════');
            console.log(pageSource);
            console.log('═══════════════════════════════════════════════════');
            console.log('');

            // Analyze for Shadow DOM
            console.log('6️⃣ Analyzing for Shadow DOM...');
            const hasShadowDom = pageSource.includes('shadow-root') || 
                                pageSource.includes('shadowRoot') ||
                                pageSource.includes('::shadow');
            console.log('   Shadow DOM detected:', hasShadowDom ? '✅ Yes' : '❌ No');
            console.log('');

            // Search for username/password fields
            console.log('7️⃣ Searching for username/password fields...');
            const usernamePatterns = [
                /username/i,
                /user/i,
                /email/i,
                /login/i,
                /id.*user/i,
                /name.*user/i
            ];
            const passwordPatterns = [
                /password/i,
                /pass/i,
                /pwd/i
            ];

            let usernameField = null;
            let passwordField = null;

            // Simple regex search in page source
            const lines = pageSource.split('\n');
            lines.forEach((line, index) => {
                if (!usernameField) {
                    for (const pattern of usernamePatterns) {
                        if (pattern.test(line)) {
                            usernameField = { line: index + 1, content: line.trim() };
                            break;
                        }
                    }
                }
                if (!passwordField) {
                    for (const pattern of passwordPatterns) {
                        if (pattern.test(line)) {
                            passwordField = { line: index + 1, content: line.trim() };
                            break;
                        }
                    }
                }
            });

            console.log('   Username field:', usernameField ? '✅ Found' : '❌ Not found');
            if (usernameField) {
                console.log('      Line:', usernameField.line);
                console.log('      Content:', usernameField.content.substring(0, 100));
            }
            console.log('   Password field:', passwordField ? '✅ Found' : '❌ Not found');
            if (passwordField) {
                console.log('      Line:', passwordField.line);
                console.log('      Content:', passwordField.content.substring(0, 100));
            }
            console.log('');
        }

        // Cleanup
        console.log('8️⃣ Cleaning up session...');
        await axios.delete(`${appiumUrl}/session/${sessionId}`);
        console.log('✅ Session deleted');
        console.log('');
        console.log('═══════════════════════════════════════════════════');
        console.log('✅ SCAN COMPLETE');
        console.log('═══════════════════════════════════════════════════');

    } catch (error) {
        console.error('❌ Error:', error.message);
        if (error.response) {
            console.error('   Response:', error.response.data);
        }
        process.exit(1);
    }
}

scanAppiumDevice();
