import $ from 'jquery';
$(document).ready(function () {
    $('.exception').click(function () {
        $('.exceptionCode').hide();
        let exceptionToDisplay = $(this).data('session');
        $('#' + exceptionToDisplay).show();
    });
});

