import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Formik, Form, Field, ErrorMessage } from 'formik';
import * as Yup from 'yup';
import { adminApi, catalogApi, errorMessage } from '../../api/client';
import { formatDateTime, formatPrice } from '../../utils';

const schema = Yup.object({
  movieId: Yup.string().required('Choose a movie'),
  theaterId: Yup.string().required('Choose a theater'),
  startTime: Yup.string().required('Start time is required')
    .test('future', 'Must be in the future', (v) => !v || new Date(v) > new Date()),
  price: Yup.number().typeError('Number').positive('Must be > 0').required('Required'),
  premiumPrice: Yup.number().typeError('Number').positive('Must be > 0').nullable(),
});

const empty = { movieId: '', theaterId: '', startTime: '', price: 150, premiumPrice: 250 };

export default function AdminShows() {
  const [shows, setShows] = useState([]);
  const [movies, setMovies] = useState([]);
  const [theaters, setTheaters] = useState([]);
  const [upcoming, setUpcoming] = useState(true);
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState('');

  const load = () => adminApi.shows(upcoming).then(setShows).catch((e) => setError(errorMessage(e)));
  useEffect(() => { load(); }, [upcoming]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => {
    adminApi.movies().then(setMovies).catch(() => {});
    catalogApi.theaters().then(setTheaters).catch(() => {});
  }, []);

  const save = async (v, { setSubmitting }) => {
    setError('');
    const body = { movieId: Number(v.movieId), theaterId: Number(v.theaterId), startTime: v.startTime.length === 16 ? `${v.startTime}:00` : v.startTime,
      price: Number(v.price), premiumPrice: v.premiumPrice === '' || v.premiumPrice == null ? null : Number(v.premiumPrice) };
    try {
      if (editing === 'new') await adminApi.createShow(body); else await adminApi.updateShow(editing.id, body);
      setEditing(null); load();
    } catch (e) { setError(errorMessage(e)); } finally { setSubmitting(false); }
  };

  const remove = async (s) => {
    if (!window.confirm(`Delete the ${formatDateTime(s.startTime)} show of ${s.movieTitle}?`)) return;
    try { await adminApi.deleteShow(s.id); load(); } catch (e) { setError(errorMessage(e)); }
  };

  const initial = editing === 'new' ? empty : editing && {
    movieId: String(editing.movieId), theaterId: String(editing.theaterId), startTime: editing.startTime.slice(0, 16),
    price: editing.price, premiumPrice: editing.premiumPrice,
  };

  return (
    <>
      <div className="d-flex flex-wrap gap-2 justify-content-between align-items-center mb-3">
        <h3 className="mb-0">Shows</h3>
        <div className="d-flex gap-2">
          <select className="form-select" value={upcoming ? 'up' : 'all'} onChange={(e) => setUpcoming(e.target.value === 'up')}>
            <option value="up">Upcoming</option><option value="all">All</option>
          </select>
          <button className="btn btn-brand text-nowrap" onClick={() => setEditing('new')}>+ Schedule Show</button>
        </div>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}
      {editing && (
        <div className="card border-0 shadow-sm p-4 mb-4">
          <h5>{editing === 'new' ? 'Schedule a Show' : 'Edit Show'}</h5>
          <Formik initialValues={initial} validationSchema={schema} onSubmit={save} enableReinitialize>
            {({ errors, touched, isSubmitting }) => {
              const cls = (n, base = 'form-control') => `${base} ${touched[n] && errors[n] ? 'is-invalid' : ''}`;
              return (
                <Form noValidate className="row g-3">
                  <div className="col-md-4"><label className="form-label">Movie</label>
                    <Field as="select" name="movieId" className={cls('movieId', 'form-select')}><option value="">Select…</option>
                      {movies.filter((m) => m.active).map((m) => <option key={m.id} value={m.id}>{m.title} ({m.durationMinutes} min)</option>)}</Field>
                    <ErrorMessage name="movieId" component="div" className="invalid-feedback" /></div>
                  <div className="col-md-4"><label className="form-label">Theater</label>
                    <Field as="select" name="theaterId" className={cls('theaterId', 'form-select')}><option value="">Select…</option>
                      {theaters.map((t) => <option key={t.id} value={t.id}>{t.name} – {t.city}</option>)}</Field>
                    <ErrorMessage name="theaterId" component="div" className="invalid-feedback" /></div>
                  <div className="col-md-4"><label className="form-label">Start time</label>
                    <Field name="startTime" type="datetime-local" className={cls('startTime')} /><ErrorMessage name="startTime" component="div" className="invalid-feedback" /></div>
                  <div className="col-md-3"><label className="form-label">Regular price ₹</label><Field name="price" type="number" className={cls('price')} /><ErrorMessage name="price" component="div" className="invalid-feedback" /></div>
                  <div className="col-md-3"><label className="form-label">Premium price ₹</label><Field name="premiumPrice" type="number" className={cls('premiumPrice')} /><ErrorMessage name="premiumPrice" component="div" className="invalid-feedback" /></div>
                  <div className="col-md-6 d-flex align-items-end gap-2 justify-content-end">
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
          <thead className="table-light"><tr><th>Date & time</th><th>Movie</th><th>Theater</th><th>Price</th><th>Seats left</th><th className="text-end">Actions</th></tr></thead>
          <tbody>
            {shows.length === 0 && <tr><td colSpan="6" className="text-center text-muted py-4">No shows.</td></tr>}
            {shows.map((s) => (
              <tr key={s.id}>
                <td className="text-nowrap">{formatDateTime(s.startTime)}</td>
                <td>{s.movieTitle}</td>
                <td>{s.theaterName}<div className="small text-muted">{s.city}</div></td>
                <td className="small">{formatPrice(s.price)} / {formatPrice(s.premiumPrice)}</td>
                <td><span className={`badge ${s.availableSeats === 0 ? 'bg-danger' : 'bg-light text-dark'}`}>{s.availableSeats} / {s.capacity}</span></td>
                <td className="text-end text-nowrap">
                  <Link className="btn btn-sm btn-outline-secondary me-2" to={`/shows/${s.id}/seats`}>Seat map</Link>
                  <Link className="btn btn-sm btn-outline-info me-2" to={`/admin/bookings?showId=${s.id}`}>Bookings</Link>
                  <button className="btn btn-sm btn-outline-primary me-2" onClick={() => { setEditing(s); window.scrollTo(0, 0); }}>Edit</button>
                  <button className="btn btn-sm btn-outline-danger" onClick={() => remove(s)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
