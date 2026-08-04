import dis

def add(x,y):
    return x + y

x = 1
y = 2

dis.dis(add)
