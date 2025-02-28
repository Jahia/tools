import * as yaml from 'js-yaml';

const validateYaml = () => {
    const sendButton = document.getElementById('submitYaml');
    const errorField = document.getElementById('errorField');
    const yamlMessage = document.getElementById('yamlMessage');
    if (sendButton.locked) {
        return;
    }

    const disableButton = () => {
        sendButton.disabled = true;
        sendButton.setAttribute('title', 'Invalid YAML');
    };

    const enableButton = () => {
        sendButton.disabled = false;
        sendButton.removeAttribute('title');
    };

    errorField.hidden = true;
    yamlMessage.innerText = '';
    const val = document.getElementById('provisioning').value;
    try {
        yaml.load(val);
        if (val.trim() === '') {
            disableButton();
            return;
        }

        enableButton();
    } catch (e) {
        yamlMessage.innerText = e.message;
        errorField.hidden = false;
        disableButton();
    }
};

const sendProvisioning = () => {
    const sendButton = document.getElementById('submitYaml');
    const waitMessage = document.getElementById('provisioningMessage');
    const resultMessage = document.getElementById('provisioningResult');

    waitMessage.setAttribute('hidden', 'true');
    resultMessage.setAttribute('hidden', 'true');
    sendButton.locked = true;

    const provisioning = document.getElementById('provisioning').value;

    const query = new XMLHttpRequest();
    query.onreadystatechange = () => {
        if (query.readyState === XMLHttpRequest.OPENED) {
            sendButton.disabled = true;
            waitMessage.removeAttribute('hidden');
        }

        if (query.readyState === XMLHttpRequest.DONE) {
            sendButton.locked = false;
            sendButton.disabled = false;
            waitMessage.setAttribute('hidden', 'true');

            if (query.status === 200) {
                resultMessage.innerText = 'Provisioning script executed successfully, check server logs for more information';
            } else {
                resultMessage.innerText = 'Provisioning script failed, check server logs for more information';
            }

            resultMessage.removeAttribute('hidden');
        }
    };

    query.open('POST', window.location.href, true);
    query.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    const encodedProvisioning = encodeURIComponent(provisioning);
    query.send('script=' + encodedProvisioning);
};

document.addEventListener('keyup', validateYaml);
document.getElementById('submitYaml').addEventListener('click', sendProvisioning);
