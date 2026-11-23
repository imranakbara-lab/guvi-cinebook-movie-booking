import { useEffect, useState } from 'react';
import { Formik, Form, Field, ErrorMessage } from 'formik';
import * as Yup from 'yup';
import { errorMessage, profileApi } from '../api/client';

export default function Profile() {
  const [profile, setProfile] = useState(null);
  const [msg, setMsg] = useState({ profile: '', password: '' });
  const [error, setError] = useState({ profile: '', password: '' });

  useEffect(() => { profileApi.get().then(setProfile).catch((e) => setError({ profile: errorMessage(e), password: '' })); }, []);

  if (!profile) return error.profile ? <div className="alert alert-danger">{error.profile}</div> : <div className="text-center py-5">Loading…</div>;

  const field = (name, label, touched, errors, type = 'text') => (
    <div className="mb-3">
      <label className="form-label">{label}</label>
      <Field name={name} type={type} className={`form-control ${touched[name] && errors[name] ? 'is-invalid' : ''}`} />
      <ErrorMessage name={name} component="div" className="invalid-feedback" />
    </div>
  );

  return (
    <div className="row g-4">
      <div className="col-md-6">
        <div className="card border-0 shadow-sm p-4">
          <h5>My Profile</h5>
          <p className="text-muted small mb-3">{profile.email} · {profile.role}</p>
          {msg.profile && <div className="alert alert-success py-2">{msg.profile}</div>}
          {error.profile && <div className="alert alert-danger py-2">{error.profile}</div>}
          <Formik initialValues={{ name: profile.name, phone: profile.phone || '' }}
                  validationSchema={Yup.object({ name: Yup.string().trim().required('Name is required'),
                    phone: Yup.string().matches(/^[0-9+\- ]{7,15}$/, { message: 'Enter a valid phone number', excludeEmptyString: true }) })}
                  onSubmit={async (values, { setSubmitting }) => {
                    setMsg({ ...msg, profile: '' }); setError({ ...error, profile: '' });
                    try {
                      const updated = await profileApi.update(values);
                      setProfile(updated);
                      const stored = JSON.parse(localStorage.getItem('user') || '{}');
                      localStorage.setItem('user', JSON.stringify({ ...stored, name: updated.name, phone: updated.phone }));
                      setMsg({ ...msg, profile: 'Profile updated' });
                    } catch (e) { setError({ ...error, profile: errorMessage(e) }); }
                    finally { setSubmitting(false); }
                  }}>
            {({ touched, errors, isSubmitting }) => (
              <Form noValidate>
                {field('name', 'Name', touched, errors)}
                {field('phone', 'Phone', touched, errors)}
                <button className="btn btn-dark" type="submit" disabled={isSubmitting}>Save</button>
              </Form>
            )}
          </Formik>
        </div>
      </div>
      <div className="col-md-6">
        <div className="card border-0 shadow-sm p-4">
          <h5 className="mb-3">Change Password</h5>
          {msg.password && <div className="alert alert-success py-2">{msg.password}</div>}
          {error.password && <div className="alert alert-danger py-2">{error.password}</div>}
          <Formik initialValues={{ currentPassword: '', newPassword: '', confirm: '' }}
                  validationSchema={Yup.object({ currentPassword: Yup.string().required('Required'),
                    newPassword: Yup.string().min(6, 'At least 6 characters').required('Required'),
                    confirm: Yup.string().oneOf([Yup.ref('newPassword')], 'Passwords do not match').required('Required') })}
                  onSubmit={async ({ currentPassword, newPassword }, { setSubmitting, resetForm }) => {
                    setMsg({ ...msg, password: '' }); setError({ ...error, password: '' });
                    try { setMsg({ ...msg, password: (await profileApi.changePassword({ currentPassword, newPassword })).message }); resetForm(); }
                    catch (e) { setError({ ...error, password: errorMessage(e) }); }
                    finally { setSubmitting(false); }
                  }}>
            {({ touched, errors, isSubmitting }) => (
              <Form noValidate>
                {field('currentPassword', 'Current password', touched, errors, 'password')}
                {field('newPassword', 'New password', touched, errors, 'password')}
                {field('confirm', 'Confirm new password', touched, errors, 'password')}
                <button className="btn btn-outline-dark" type="submit" disabled={isSubmitting}>Change password</button>
              </Form>
            )}
          </Formik>
        </div>
      </div>
    </div>
  );
}
