import {NodeSSH} from 'node-ssh';

interface connection {
    hostname: string
    port: string
    username: string
    password: string
}

const sshCommand = (commands: Array<string>, connection: connection) => {
    return new Promise((resolve, reject) => {
        const ssh = new NodeSSH();
        console.log('[SSH] connection to:', connection.hostname);
        console.log('[SSH] connection username:', connection.username);
        console.log('[SSH] commands:', commands);
        console.log('[SSH] ', connection);
        ssh.connect({
            host: connection.hostname,
            port: connection.port,
            username: connection.username,
            password: connection.password
        })
            .then(() => {
                ssh.exec(commands.join(';'), [])
                    .then(function (result) {
                        resolve(result); // Resolve to command result
                    })
                    .catch(reason => {
                        console.error('[SSH] Failed to execute commands: ', commands);
                        console.error('[SSH] Reason: ', reason);
                        reject(reason);
                    });
            })
            .catch(reason => {
                console.error('[SSH] Failed to execute commands: ', commands);
                console.error('[SSH] Reason: ', reason);
                reject(reason);
            });
    });
};

module.exports = sshCommand;
