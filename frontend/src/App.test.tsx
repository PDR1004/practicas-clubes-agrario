import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App.tsx'

describe('App', () => {
  it('muestra el nombre del sistema', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Clubes Agrario' })).toBeInTheDocument()
  })
})
