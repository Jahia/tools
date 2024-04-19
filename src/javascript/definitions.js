import $ from 'jquery';

$(document).ready(function () {
    $('.delete-definitions img').click(function (event) {
        const packageName = event.target.attributes['data-package'].value;

        // eslint-disable-next-line no-alert
        if (confirm(`You are about to delete all nodetypes from module ${packageName} and all associated content. Continue?`)) {
            $('#action').val('deleteModule');
            $('#module').val(packageName);
            $('#navigateForm').submit();
        }
    });

    $('.delete-nodetype img').click(function (event) {
        const nodetype = event.target.attributes['data-nodetype'].value;

        // eslint-disable-next-line no-alert
        if (confirm(`You are about to delete the nodetype ${nodetype} and all associated content. Continue?`)) {
            $('#action').val('deleteNodeType');
            $('#module').val(event.target.attributes['data-package'].value);
            $('#nodetype').val(event.target.attributes['data-nodetype'].value);
            $('#navigateForm').submit();
        }
    });
});

