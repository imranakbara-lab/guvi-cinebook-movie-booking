import { useEffect, useState } from 'react';
import { Formik, Form, Field, ErrorMessage } from 'formik';
import * as Yup from 'yup';
import { adminApi, catalogApi, errorMessage } from '../../api/client';

const schema = Yup.object({
  name: Yup.string().trim().required('Name is required'),
  city: Yup.string().trim().required('City is required'),
  totalRows: Yup.number().typeError('Number').integer().min(1).max(26, 'Max 26 rows').required('Required'),
  seatsPerRow: Yup.number().typeError('Number').integer().min(1).max(40, 'Max 40').required('Required'),
  premiumRows: Yup.number().typeError('Number').integer().min(0).required('Required')
    .test('le-rows', 'Cannot exceed total rows', function (v) { return v <= Number(this.parent.totalRows || 0); }),
});

const empty = { name: '', city: '', address: '', totalRows: 10, seatsPerRow: 12, premiumRows: 2 };

export default function AdminTheaters() {
  const [theaters, setTheaters] = useState([]);
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState('');

  const load = () => catalogApi.theaters().then(setTheaters).catch((e) => setError(errorMessage(e)));
  useEffect(() => { load(); }, []);

  const save = async (v, { setSubmitting }) => {
    setError('');
    const body = { ...v, totalRows: Number(v.totalRows), seatsPerRow: Number(v.seatsPerRow), premiumRows: Number(v.premiumRows) };
    try {
      if (editing === 'new') await adminApi.createTheater(body); else await adminApi.updateTheater(editing.id, body);
      setEditing(null); load();
    } catch (e) { setError(errorMessage(e)); } finally { setSubmitting(false); }
  };

  const remove = async (t) => {
    if (!window.confirm(`Delete "${t.name}"?`)) return;
    try { await adminApi.deleteTheater(t.id); load(); } catch (e) { setError(errorMessage(e)); }
  };

  return (
    <>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h3 className="mb-0">Theaters</h3>
        <button className="btn btn-brand" onClick={() => setEditing('new')}>+ Add Theater</button>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}
      {editing && (
        <div className="card border-0 shadow-sm p-4 mb-4">
          <h5>{editing === 'new' ? 'New Theater' : `Edit: ${editing.name}`}</h5>
          <Formik initialValues={editing === 'new' ? empty : { ...empty, ...editing, address: editing.address || '' }} validationSchema={schema} onSubmit={save} enableReinitialize>
            {({ errors, touched, isSubmitting, values }) => {
              const cls = (n) => `form-control ${touched[n] && errors[n] ? 'is-invalid' : ''}`;
              const f = (n, l, type = 'text', col = 'col-md-4') => (
                <div className={col}><label className="form-label">{l}</label><Field name={n} type={type} className={cls(n)} /><ErrorMessage name={n} component="div" className="invalid-feedback" /></div>
              );
              return (
                <Form noValidate className="row g-3">
                  {f('name', 'Name (screen)', 'text', 'col-md-5')}{f('city', 'City', 'text', 'col-md-3')}{f('address', 'Address', 'text', 'col-md-4')}
                  {f('totalRows', 'Rows (A-Z)', 'number')}{f('seatsPerRow', 'Seats per row', 'number')}{f('premiumRows', 'Premium rows (at the back)', 'number')}
                  <div className="col-12 d-flex align-items-center gap-3">
                    <span className="text-muted small">Capacity: {Number(values.totalRows || 0) * Number(values.seatsPerRow || 0)} seats</span>
                    <span className="flex-grow-1" />
                    <button type="button" className="btn btn-outline-secondary" onClick={() => setEditing(null)}>Cancel</button>
                    <button type="submit" className="btn btn-dark" disabled={isSubmitting}>Save</button>
                  </div>
                </Form>
              );
            }}
          </Formik>
        </div>
      )}
      <div className="table-responsive card border-0 shadow-sm">
        <table className="table table-hover align-middle mb-0">
          <thead className="table-light"><tr><th>Name</th><th>City</th><th>Address</th><th>Layout</th><th>Capacity</th><th className="text-end">Actions</th></tr></thead>
          <tbody>
            {theaters.map((t) => (
              <tr key={t.id}>
                <td>{t.name}</td><td>{t.city}</td><td className="small">{t.address}</td>
                <td>{t.totalRows} rows × {t.seatsPerRow}{t.premiumRows > 0 && <span className="small text-muted"> ({t.premiumRows} premium)</span>}</td>
                <td>{t.capacity}</td>
                <td className="text-end text-nowrap">
                  <button className="btn btn-sm btn-outline-primary me-2" onClick={() => setEditing(t)}>Edit</button>
                  <button className="btn btn-sm btn-outline-danger" onClick={() => remove(t)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
