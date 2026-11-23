import { useEffect, useState } from 'react';
import { Formik, Form, Field, ErrorMessage } from 'formik';
import * as Yup from 'yup';
import { adminApi, errorMessage } from '../../api/client';
import { POSTER_FALLBACK } from '../../utils';

const schema = Yup.object({
  title: Yup.string().trim().required('Title is required'),
  genre: Yup.string().trim().required('Genre is required'),
  durationMinutes: Yup.number().typeError('Minutes').integer().min(30, 'Min 30').max(300, 'Max 300').required('Required'),
  rating: Yup.number().typeError('Number').min(0).max(10, '0 - 10').nullable(),
  posterUrl: Yup.string().url('Enter a valid URL').nullable(),
});

const empty = { title: '', description: '', genre: '', language: 'Tamil', durationMinutes: 120, certificate: 'UA', rating: '', posterUrl: '', releaseDate: '', active: true };

export default function AdminMovies() {
  const [movies, setMovies] = useState([]);
  const [editing, setEditing] = useState(null);   // movie object or 'new'
  const [error, setError] = useState('');

  const load = () => adminApi.movies().then(setMovies).catch((e) => setError(errorMessage(e)));
  useEffect(() => { load(); }, []);

  const save = async (values, { setSubmitting }) => {
    setError('');
    const body = { ...values, durationMinutes: Number(values.durationMinutes), rating: values.rating === '' ? null : Number(values.rating), releaseDate: values.releaseDate || null };
    try {
      if (editing === 'new') await adminApi.createMovie(body); else await adminApi.updateMovie(editing.id, body);
      setEditing(null);
      load();
    } catch (e) { setError(errorMessage(e)); } finally { setSubmitting(false); }
  };

  const remove = async (m) => {
    if (!window.confirm(`Delete "${m.title}"?`)) return;
    try { await adminApi.deleteMovie(m.id); load(); } catch (e) { setError(errorMessage(e)); }
  };

  const initial = editing === 'new' ? empty : editing && { ...empty, ...editing, rating: editing.rating ?? '', posterUrl: editing.posterUrl || '', description: editing.description || '', releaseDate: editing.releaseDate || '', language: editing.language || '', certificate: editing.certificate || '' };

  return (
    <>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h3 className="mb-0">Movies</h3>
        <button className="btn btn-brand" onClick={() => setEditing('new')}>+ Add Movie</button>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}

      {editing && (
        <div className="card border-0 shadow-sm p-4 mb-4">
          <h5>{editing === 'new' ? 'New Movie' : `Edit: ${editing.title}`}</h5>
          <Formik initialValues={initial} validationSchema={schema} onSubmit={save} enableReinitialize>
            {({ errors, touched, isSubmitting }) => {
              const cls = (n) => `form-control ${touched[n] && errors[n] ? 'is-invalid' : ''}`;
              return (
                <Form noValidate className="row g-3">
                  <div className="col-md-6"><label className="form-label">Title</label><Field name="title" className={cls('title')} /><ErrorMessage name="title" component="div" className="invalid-feedback" /></div>
                  <div className="col-md-3"><label className="form-label">Genre</label><Field name="genre" className={cls('genre')} placeholder="Action" /><ErrorMessage name="genre" component="div" className="invalid-feedback" /></div>
                  <div className="col-md-3"><label className="form-label">Language</label><Field name="language" className="form-control" /></div>
                  <div className="col-md-3"><label className="form-label">Duration (min)</label><Field name="durationMinutes" type="number" className={cls('durationMinutes')} /><ErrorMessage name="durationMinutes" component="div" className="invalid-feedback" /></div>
                  <div className="col-md-3"><label className="form-label">Certificate</label>
                    <Field as="select" name="certificate" className="form-select"><option value="">—</option><option>U</option><option>UA</option><option>A</option></Field></div>
                  <div className="col-md-3"><label className="form-label">Rating (0-10)</label><Field name="rating" type="number" step="0.1" className={cls('rating')} /><ErrorMessage name="rating" component="div" className="invalid-feedback" /></div>
                  <div className="col-md-3"><label className="form-label">Release date</label><Field name="releaseDate" type="date" className="form-control" /></div>
                  <div className="col-12"><label className="form-label">Poster image URL</label><Field name="posterUrl" className={cls('posterUrl')} placeholder="https://…" /><ErrorMessage name="posterUrl" component="div" className="invalid-feedback" /></div>
                  <div className="col-12"><label className="form-label">Description</label><Field as="textarea" rows="3" name="description" className="form-control" /></div>
                  <div className="col-12 d-flex align-items-center gap-3">
                    <div className="form-check form-switch"><Field type="checkbox" name="active" className="form-check-input" id="active" /><label className="form-check-label" htmlFor="active">Active (visible to users)</label></div>
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
          <thead className="table-light"><tr><th></th><th>Title</th><th>Genre</th><th>Language</th><th>Duration</th><th>Rating</th><th>Status</th><th className="text-end">Actions</th></tr></thead>
          <tbody>
            {movies.map((m) => (
              <tr key={m.id}>
                <td><img src={m.posterUrl || POSTER_FALLBACK} alt="" style={{ width: 36, height: 54, objectFit: 'cover' }} className="rounded" /></td>
                <td>{m.title} {m.certificate && <span className="badge bg-secondary">{m.certificate}</span>}</td>
                <td>{m.genre}</td><td>{m.language}</td><td>{m.durationMinutes} min</td><td>{m.rating ?? '-'}</td>
                <td><span className={`badge ${m.active ? 'bg-success' : 'bg-secondary'}`}>{m.active ? 'Active' : 'Inactive'}</span></td>
                <td className="text-end text-nowrap">
                  <button className="btn btn-sm btn-outline-primary me-2" onClick={() => { setEditing(m); window.scrollTo(0, 0); }}>Edit</button>
                  <button className="btn btn-sm btn-outline-danger" onClick={() => remove(m)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
