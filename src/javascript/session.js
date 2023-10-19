import $ from 'jquery';
$(document).ready(function () {
    $('.exception').click(function () {
        $('.exceptionCode').hide();
        var exceptionToDisplay = $(this).data('session');
        $('#' + exceptionToDisplay).show();
    });
});

