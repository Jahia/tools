import 'bootstrap/dist/js/bootstrap.bundle.min';
import Datatable from 'datatables.net';
import 'datatables.net-bs5/js/dataTables.bootstrap5.min';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'datatables.net-bs5/css/dataTables.bootstrap5.min.css';

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-table="dataTable"]')
        .forEach(dataTable => {
            // Instanciate a datatable (ignore sonar warings)
            // eslint-disable-next-line no-new
            new Datatable(dataTable, {
                pageLength: 300,
                lengthMenu: [10, 25, 50, 100, 300]
            }); // NOSONAR
        });
});
