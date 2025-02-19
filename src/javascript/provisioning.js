import * as yaml from 'js-yaml';

const validateYaml = () => {
    const sendButton = document.getElementById('submitYaml');
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

    const val = document.getElementById('provisioning').value;
    try {
        yaml.load(val);
        if (val.trim() === '') {
            disableButton();
            return;
        }

        enableButton();
    } catch (_) {
        disableButton();
    }
};

document.addEventListener('keyup', validateYaml);
