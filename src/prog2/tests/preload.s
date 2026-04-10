	.data
_scratch:
	.space 64
	.text
.import "tmp_2547981358.s"
	.globl __crt0
__crt0:
	jal mainuidtanxgflhvcfirdn
	or a1 a0 zero
	jal print_integer
	li a0 10
	ecall
	.globl print_integer
print_integer:
	li a0 1
	ecall
	jr ra
	.globl print_string
print_string:
	li a0 4
	ecall
	jr ra
	.globl print_char
print_char:
	li a0 11
	ecall
	jr ra
	.globl malloc
malloc:
	or a1 a0 zero
	li a0 9
	ecall
	jr ra
	.globl get_scratch
get_scratch:
	la a0 _scratch
	jr ra
