import { useEffect, useState, type FormEvent } from "react";

import { Layout } from "@/components/Layout";
import { Field, inputClass } from "@/components/FormField";
import { PanelHeader } from "@/components/PageBits";

import {
  getMyProfile,
  updateMyProfile,
  changePassword,
  type StudentProfile,
} from "@/api";

import { Loader2, Save, Lock, UserCircle, MapPin, Users, Info, AlertCircle, CheckCircle2 } from "lucide-react";

export function StudentProfilePage() {

  const [student, setStudent] =useState<StudentProfile | null>(null)
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [password, setPassword] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });

  async function load() {
    try {
      const data = await getMyProfile();
      setStudent(data);
    } catch {
      setError("Unable to load profile.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  if (loading) {
    return (
      <Layout>
        <div className="flex items-center gap-2 text-sm text-slate-dim">
          <Loader2 size={16} className="animate-spin" />
          Loading profile...
        </div>
      </Layout>
    );
  }

  async function saveProfile(e: FormEvent) {
    e.preventDefault();
    if (!student) return;

    setSaving(true);
    setMessage("");

    try {
      await updateMyProfile(student);
      setMessage("Profile updated successfully.");
    } catch {
      setError("Unable to update profile.");
    } finally {
      setSaving(false);
    }
  }

  async function savePassword(e: FormEvent) {
    e.preventDefault();

    if (password.newPassword !== password.confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setPasswordSaving(true);
    setError("");

    try {
      await changePassword({
    oldPassword: password.currentPassword,
    newPassword: password.newPassword
})
      setMessage("Password updated successfully.");
      setPassword({
        currentPassword: "",
        newPassword: "",
        confirmPassword: "",
      });
    } catch {
      setError("Unable to change password.");
    } finally {
      setPasswordSaving(false);
    }
  }

  return (
    <Layout>
      <h1 className="font-display text-2xl font-medium text-ink">My Profile</h1>
      <p className="mt-1 text-sm text-slate-dim">Update your personal information.</p>

      {error && (
        <div className="mt-6 flex items-start gap-2 rounded-lg border border-red-300 bg-red-50 p-3 text-sm text-red-600">
          <AlertCircle size={16} className="mt-0.5 shrink-0" />
          {error}
        </div>
      )}

      {message && (
        <div className="mt-6 flex items-start gap-2 rounded-lg border border-green-300 bg-green-50 p-3 text-sm text-green-700">
          <CheckCircle2 size={16} className="mt-0.5 shrink-0" />
          {message}
        </div>
      )}

      <form onSubmit={saveProfile} className="mt-6 space-y-8">
        <div className="paper rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]">
          <PanelHeader icon={UserCircle} title="Personal Information" />
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Admission No">
              <input value={student?.admissionNo} disabled className={inputClass} />
            </Field>

            <Field label="Roll Number">
              <input value={student?.rollNumber} disabled className={inputClass} />
            </Field>

            <Field label="First Name">
              <input value={student?.firstName} disabled className={inputClass} />
            </Field>

            <Field label="Last Name">
              <input value={student?.lastName} disabled className={inputClass} />
            </Field>

            <Field label="Email">
              <input
                type="email"
                value={student?.email}
                disabled
                className={inputClass}
              />
            </Field>

            <Field label="Phone">
              <input
                value={student?.phone ?? ""}
                onChange={(e) =>
                  setStudent({
                    ...student!,
                    phone: e.target.value,
                  })
                }
                className={inputClass}
              />
            </Field>

            <Field label="Gender">
              <input value={student?.gender} disabled className={inputClass} />
            </Field>

            <Field label="Date of Birth">
              <input value={student?.dateOfBirth} disabled className={inputClass} />
            </Field>
          </div>
        </div>

        <div className="paper rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]">
          <PanelHeader icon={MapPin} title="Address Information" />
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Address" className="sm:col-span-2">
              <textarea
                rows={3}
                value={student?.address ?? ""}
                onChange={(e) =>
                  setStudent({
                    ...student!,
                    address: e.target.value,
                  })
                }
                className={inputClass}
              />
            </Field>

            <Field label="City">
              <input
                value={student?.city ?? ""}
                onChange={(e) =>
                  setStudent({
                    ...student!,
                    city: e.target.value,
                  })
                }
                className={inputClass}
              />
            </Field>

            <Field label="State">
              <input
                value={student?.state ?? ""}
                onChange={(e) =>
                  setStudent({
                    ...student!,
                    state: e.target.value,
                  })
                }
                className={inputClass}
              />
            </Field>

            <Field label="Country">
              <input
                value={student?.country ?? ""}
                onChange={(e) =>
                  setStudent({
                    ...student!,
                    country: e.target.value,
                  })
                }
                className={inputClass}
              />
            </Field>

            <Field label="Pincode">
              <input
                value={student?.pincode ?? ""}
                onChange={(e) =>
                  setStudent({
                    ...student!,
                    pincode: e.target.value,
                  })
                }
                className={inputClass}
              />
            </Field>
          </div>
        </div>

        <div className="paper rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]">
          <PanelHeader icon={Users} title="Parent Information" />
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Father Name">
              <input value={student?.fatherName ?? ""} disabled className={inputClass} />
            </Field>

            <Field label="Mother Name">
              <input value={student?.motherName ?? ""} disabled className={inputClass} />
            </Field>

            <Field label="Parent Phone">
              <input
                value={student?.parentPhone ?? ""}
                onChange={(e) =>
                  setStudent({
                    ...student!,
                    parentPhone: e.target.value,
                  })
                }
                className={inputClass}
              />
            </Field>

            <Field label="Parent Email">
              <input
                type="email"
                value={student?.parentEmail ?? ""}
                onChange={(e) =>
                  setStudent({
                    ...student!,
                    parentEmail: e.target.value,
                  })
                }
                className={inputClass}
              />
            </Field>
          </div>
        </div>

        <div className="paper rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]">
          <PanelHeader icon={Info} title="Other Details" />
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Blood Group">
              <input value={student?.bloodGroup ?? ""} disabled className={inputClass} />
            </Field>

            <Field label="Category">
              <input value={student?.category ?? ""} disabled className={inputClass} />
            </Field>

            <Field label="Nationality">
              <input value={student?.nationality ?? ""} disabled className={inputClass} />
            </Field>
          </div>

          <div className="mt-4">
            <Field label="Photo URL">
              <input
                value={student?.photoUrl ?? ""}
                onChange={(e) =>
                  setStudent({
                    ...student!,
                    photoUrl: e.target.value,
                  })
                }
                className={inputClass}
              />
            </Field>
          </div>
        </div>

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={saving}
            className="flex w-full items-center justify-center gap-2 rounded-md bg-brass px-5 py-2 text-sm font-medium text-white hover:bg-brass/90 disabled:opacity-60 sm:w-auto"
          >
            {saving && <Loader2 size={16} className="animate-spin" />}
            <Save size={16} />
            Save Changes
          </button>
        </div>
      </form>

      <div className="paper mt-8 rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]">
        <PanelHeader icon={Lock} title="Change Password" />

        <form onSubmit={savePassword} className="space-y-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <Field label="Current Password">
              <input
                type="password"
                value={password.currentPassword}
                onChange={(e) =>
                  setPassword({
                    ...password,
                    currentPassword: e.target.value,
                  })
                }
                className={inputClass}
              />
            </Field>

            <Field label="New Password">
              <input
                type="password"
                value={password.newPassword}
                onChange={(e) =>
                  setPassword({
                    ...password,
                    newPassword: e.target.value,
                  })
                }
                className={inputClass}
              />
            </Field>

            <Field label="Confirm Password">
              <input
                type="password"
                value={password.confirmPassword}
                onChange={(e) =>
                  setPassword({
                    ...password,
                    confirmPassword: e.target.value,
                  })
                }
                className={inputClass}
              />
            </Field>
          </div>

          <div className="flex justify-end">
            <button
              type="submit"
              disabled={passwordSaving}
              className="flex w-full items-center justify-center gap-2 rounded-md bg-brass px-5 py-2 text-sm font-medium text-white hover:bg-brass/90 disabled:opacity-60 sm:w-auto"
            >
              {passwordSaving && <Loader2 size={16} className="animate-spin" />}
              Update Password
            </button>
          </div>
        </form>
      </div>
    </Layout>
  );
}
