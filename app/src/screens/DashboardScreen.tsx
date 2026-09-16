import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { colors, radius, spacing } from '../theme';

type Props = { navigation: any };

const modules = [
  ['Classes', 'View your current classes'],
  ['Attendance', 'Check attendance and holidays'],
  ['Assignments', 'Track coursework and submissions'],
  ['Timetable', 'See your weekly schedule'],
  ['Profile', 'View your account'],
];

export function DashboardScreen({ navigation }: Props) {
  return (
    <ScrollView style={styles.page} contentContainerStyle={styles.content}>
      <Text style={styles.eyebrow}>EDUSPHERE MOBILE</Text>
      <Text style={styles.title}>Your campus, in your pocket.</Text>
      <Text style={styles.muted}>Access the same academic data used by the EduSphere web ERP.</Text>
      <View style={styles.hero}>
        <Text style={styles.heroLabel}>STUDENT PORTAL</Text>
        <Text style={styles.heroTitle}>Good to see you.</Text>
        <Text style={styles.heroText}>Choose a module below to continue.</Text>
      </View>
      {modules.map(([title, description]) => (
        <Pressable key={title} style={styles.card} onPress={() => navigation.navigate(title)}>
          <View style={styles.icon}><Text style={styles.iconText}>{title[0]}</Text></View>
          <View style={styles.cardBody}><Text style={styles.cardTitle}>{title}</Text><Text style={styles.cardText}>{description}</Text></View>
          <Text style={styles.arrow}>›</Text>
        </Pressable>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: colors.background },
  content: { padding: spacing.lg, paddingTop: spacing.xl, paddingBottom: spacing.xxl },
  eyebrow: { color: colors.primary, fontSize: 12, fontWeight: '800', letterSpacing: 1.5 },
  title: { color: colors.text, fontSize: 30, lineHeight: 37, fontWeight: '800', marginTop: spacing.sm },
  muted: { color: colors.muted, fontSize: 15, lineHeight: 23, marginTop: spacing.sm },
  hero: { backgroundColor: colors.primary, borderRadius: radius.card, padding: spacing.lg, marginTop: spacing.xl, marginBottom: spacing.md },
  heroLabel: { color: '#d9f2dc', fontSize: 11, fontWeight: '800', letterSpacing: 1.3 },
  heroTitle: { color: colors.card, fontSize: 24, fontWeight: '800', marginTop: spacing.xs },
  heroText: { color: '#e8f5e9', fontSize: 14, marginTop: spacing.xs },
  card: { backgroundColor: colors.card, borderRadius: radius.card, padding: spacing.md, marginTop: spacing.sm, flexDirection: 'row', alignItems: 'center', shadowColor: colors.text, shadowOpacity: 0.05, shadowRadius: 12, shadowOffset: { width: 0, height: 3 }, elevation: 2 },
  icon: { width: 48, height: 48, borderRadius: 16, backgroundColor: colors.lightGreen, alignItems: 'center', justifyContent: 'center' },
  iconText: { color: colors.primary, fontSize: 19, fontWeight: '800' },
  cardBody: { flex: 1, marginLeft: spacing.md },
  cardTitle: { color: colors.text, fontSize: 16, fontWeight: '800' },
  cardText: { color: colors.muted, fontSize: 13, marginTop: 3 },
  arrow: { color: colors.primary, fontSize: 28, marginLeft: spacing.sm },
});
