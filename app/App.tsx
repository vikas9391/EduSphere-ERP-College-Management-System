import React, { useState } from 'react';
import { ActivityIndicator, Alert, Pressable, SafeAreaView, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { colors, radius, spacing } from './src/theme';
import { login, logout } from './src/api';

export default function App() {
  const [collegeCode, setCollegeCode] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [session, setSession] = useState<{ role?: string; username?: string } | null>(null);

  async function handleLogin() {
    if (!collegeCode || !username || !password) {
      Alert.alert('Missing details', 'Enter college code, username and password.');
      return;
    }
    try {
      setLoading(true);
      const result = await login(collegeCode, username, password);
      setSession({ role: result.role, username: result.username ?? username });
    } catch (error) {
      Alert.alert('Login failed', error instanceof Error ? error.message : 'Please try again.');
    } finally {
      setLoading(false);
    }
  }

  async function handleLogout() {
    await logout();
    setSession(null);
    setPassword('');
  }

  if (session) {
    return (
      <SafeAreaView style={styles.safe}>
        <StatusBar style="dark" />
        <ScrollView contentContainerStyle={styles.container}>
          <View style={styles.brandRow}>
            <View style={styles.logo}><Text style={styles.logoText}>E</Text></View>
            <View><Text style={styles.brand}>EduSphere</Text><Text style={styles.subtitle}>College ERP</Text></View>
          </View>
          <View style={styles.hero}>
            <Text style={styles.eyebrow}>WELCOME BACK</Text>
            <Text style={styles.title}>Your campus, in your pocket.</Text>
            <Text style={styles.muted}>Signed in as {session.username} · {session.role ?? 'USER'}</Text>
          </View>
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Mobile app foundation</Text>
            <Text style={styles.muted}>The app is connected to the same EduSphere Spring Boot API. Role-specific dashboards and academic modules can now be added without changing the backend.</Text>
          </View>
          <Pressable style={styles.buttonSecondary} onPress={handleLogout}><Text style={styles.buttonSecondaryText}>Sign out</Text></Pressable>
        </ScrollView>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar style="dark" />
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <View style={styles.brandRow}>
          <View style={styles.logo}><Text style={styles.logoText}>E</Text></View>
          <View><Text style={styles.brand}>EduSphere</Text><Text style={styles.subtitle}>College ERP</Text></View>
        </View>
        <View style={styles.hero}>
          <Text style={styles.eyebrow}>MOBILE CAMPUS</Text>
          <Text style={styles.title}>Everything your college needs.</Text>
          <Text style={styles.muted}>Use the same ERP account on mobile. Classes, attendance, assignments, timetable and more will use the existing backend.</Text>
        </View>
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Sign in</Text>
          <Text style={styles.label}>College code</Text>
          <TextInput value={collegeCode} onChangeText={setCollegeCode} placeholder="e.g. MALLAREDDY" placeholderTextColor={colors.muted} style={styles.input} autoCapitalize="characters" />
          <Text style={styles.label}>Username</Text>
          <TextInput value={username} onChangeText={setUsername} placeholder="Username" placeholderTextColor={colors.muted} style={styles.input} autoCapitalize="none" />
          <Text style={styles.label}>Password</Text>
          <TextInput value={password} onChangeText={setPassword} placeholder="Password" placeholderTextColor={colors.muted} style={styles.input} secureTextEntry />
          <Pressable style={styles.button} onPress={handleLogin} disabled={loading}>
            {loading ? <ActivityIndicator color={colors.card} /> : <Text style={styles.buttonText}>Sign in</Text>}
          </Pressable>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  container: { flexGrow: 1, padding: spacing.lg, paddingTop: spacing.xl },
  brandRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginBottom: spacing.xl },
  logo: { width: 48, height: 48, borderRadius: 16, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
  logoText: { color: colors.card, fontSize: 26, fontWeight: '800' },
  brand: { color: colors.text, fontSize: 23, fontWeight: '800' },
  subtitle: { color: colors.muted, fontSize: 12, marginTop: 1 },
  hero: { marginBottom: spacing.lg },
  eyebrow: { color: colors.primary, fontSize: 12, fontWeight: '800', letterSpacing: 1.5, marginBottom: spacing.sm },
  title: { color: colors.text, fontSize: 32, lineHeight: 39, fontWeight: '800', marginBottom: spacing.sm },
  muted: { color: colors.muted, fontSize: 15, lineHeight: 23 },
  card: { backgroundColor: colors.card, borderRadius: radius.card, padding: spacing.lg, shadowColor: colors.text, shadowOpacity: 0.06, shadowRadius: 14, shadowOffset: { width: 0, height: 3 }, elevation: 2 },
  cardTitle: { color: colors.text, fontSize: 21, fontWeight: '800', marginBottom: spacing.lg },
  label: { color: colors.text, fontSize: 13, fontWeight: '700', marginBottom: spacing.xs, marginTop: spacing.sm },
  input: { height: 54, borderWidth: 1, borderColor: colors.border, borderRadius: radius.input, paddingHorizontal: spacing.md, color: colors.text, backgroundColor: colors.background },
  button: { height: 54, borderRadius: radius.button, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center', marginTop: spacing.lg },
  buttonText: { color: colors.card, fontSize: 16, fontWeight: '800' },
  buttonSecondary: { height: 54, borderRadius: radius.button, backgroundColor: colors.lightGreen, alignItems: 'center', justifyContent: 'center', marginTop: spacing.lg },
  buttonSecondaryText: { color: colors.primaryDark, fontSize: 16, fontWeight: '800' },
});
