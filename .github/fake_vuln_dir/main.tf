# fake_vuln_dir/main.tf
resource "aws_security_group" "bad_sg" {
  name        = "bad_sg"
  description = "Allow all inbound traffic"

  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
