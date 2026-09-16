import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { colors, radius, spacing } from '../theme';

export function ClassesScreen() {
  return <ScrollView style={styles.page} contentContainerStyle={styles.content}><Text style={styles.title}>My Classes</Text><Text style={styles.muted}>Classes and ClassSubject data will be loaded from the existing ERP API.</Text><View style={styles.empty}><Text style={styles.emptyTitle}>Class data connection</Text><Text style={styles.muted}>The mobile screen is ready for the authenticated ClassEnrollment/ClassSubject endpoints.</Text></View></ScrollView>;
}
const styles = StyleSheet.create({ page:{flex:1,backgroundColor:colors.background},content:{padding:spacing.lg,paddingTop:spacing.xl},title:{fontSize:28,fontWeight:'800',color:colors.text},muted:{fontSize:15,lineHeight:23,color:colors.muted,marginTop:spacing.sm},empty:{backgroundColor:colors.card,borderRadius:radius.card,padding:spacing.lg,marginTop:spacing.xl},emptyTitle:{fontSize:18,fontWeight:'800',color:colors.text} });
