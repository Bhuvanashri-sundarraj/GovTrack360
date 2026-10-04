import React from 'react'

export default function Badge({ status }) {
  const isActive = String(status).toUpperCase() === 'ACTIVE'
  return (
    <span className={isActive ? 'badge-active' : 'badge-inactive'}>
      {isActive ? 'Active' : 'Inactive'}
    </span>
  )
}
