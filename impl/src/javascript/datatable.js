import 'bootstrap/dist/js/bootstrap.bundle.min';
import Datatable from 'datatables.net';
import 'datatables.net-bs5/js/dataTables.bootstrap5.min';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'datatables.net-bs5/css/dataTables.bootstrap5.min.css';

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-table="dataTable"]')
        .forEach(dataTable => {
            // Instantiate a datatable (ignore sonar warnings)
            // eslint-disable-next-line no-new
            new Datatable(dataTable); // NOSONAR
        });
    document.querySelectorAll('[data-table="dataTableModulesBrowser"]')
        .forEach(dataTable => {
            // Instantiate a datatable (ignore sonar warnings)
            // eslint-disable-next-line no-new
            new Datatable(dataTable, {paging: false}); // NOSONAR
        });
    document.querySelectorAll('[data-table="dataTableDefinitionsBrowser"]')
        .forEach(dataTable => {
            // Instantiate a datatable (ignore sonar warnings)
            // eslint-disable-next-line no-new
            new Datatable(dataTable, { // NOSONAR
                paging: false,
                columns: [
                    null,
                    null,
                    {
                        // Tweak the "rendering" of the 3rd columns:
                        // Only return the <ol> > <li> text (and exclude, for instance, the whole '<div style="display:none">' used to show details of a node type definition)
                        render: function (data, type) {
                            if (type === 'filter') {
                                // Parse HTML and extract <ol> > <li> text
                                const temp = document.createElement('div');
                                temp.innerHTML = data;
                                const items = temp.querySelectorAll('ol > li');
                                return Array.from(items).map(li => li.textContent.trim()).join(' ');
                            }

                            return data;
                        }
                    }
                ]
            });
        });
});
