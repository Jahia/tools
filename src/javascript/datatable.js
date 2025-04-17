import 'bootstrap/dist/js/bootstrap.bundle.min';
import Datatable from 'datatables.net';
import 'datatables.net-bs5/js/dataTables.bootstrap5.min';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'datatables.net-bs5/css/dataTables.bootstrap5.min.css';

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-table="dataTable"]')
        .forEach(dataTable => {
            // eslint-disable-next-line no-unused-vars
            const table = new Datatable(dataTable);
        });
});
