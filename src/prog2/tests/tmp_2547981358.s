	.globl mainuidtanxgflhvcfirdn
mainuidtanxgflhvcfirdn:
	addi    sp sp -128
	sw      ra 124(sp)
	sw      s0 120(sp)
	addi    s0 sp 128
	jal     get_scratch
	sw      a0 -12(s0)
	lw      a0 -12(s0)
	sw      a0 -16(s0)
	lw      a0 -12(s0)
	sw      a0 -20(s0)
	addi    a0 zero 100
	addi    sp sp -4
	sw      a0 0(sp)
	lw      a0 -20(s0)
	addi    sp sp -4
	sw      a0 0(sp)
	addi    a0 zero 4
	lw      t0 0(sp)
	addi    sp sp 4
	add     a0 t0 a0
	lw      t0 0(sp)
	addi    sp sp 4
	sw      t0 0(a0)
	lw      a0 -16(s0)
	addi    sp sp -4
	sw      a0 0(sp)
	addi    a0 zero 1
	lw      t0 0(sp)
	addi    sp sp 4
	slli    a0 a0 2
	add     a0 t0 a0
	lw      a0 0(a0)
	j       .Lepilogue_mainuidtanxgflhvcfirdn.0
.Lepilogue_mainuidtanxgflhvcfirdn.0:
	lw      ra 124(sp)
	lw      s0 120(sp)
	addi    sp sp 128
	jr      ra
